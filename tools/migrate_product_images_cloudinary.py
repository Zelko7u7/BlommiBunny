"""Migrate catalog photos to Cloudinary without changing commercial data.

Run prepare, upload, apply, verify in order. Private plans, backups and receipts
stay in .local/cloudinary-catalog-migration. Existing local originals are retained.
Credentials are read only from environment or the ignored Cloudinary properties.
"""
from pathlib import Path
import argparse, concurrent.futures, hashlib, json, mimetypes
import os, re, subprocess, time, urllib.request, urllib.error, urllib.parse, uuid

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'.local/cloudinary-catalog-migration'
PUBLIC=ROOT/'BloomiFront/public'
COLLECTION=ROOT/'BloomiFront/src/data/product-images.json'
# Use the repository database helper; this tool does not depend on old sessions.
from update_product_media import Database, quote

def save(name,value):
    OUT.mkdir(parents=True,exist_ok=True)
    target=OUT/name; temporary=target.with_suffix(target.suffix+'.tmp')
    temporary.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');temporary.replace(target)
def read(name):return json.loads((OUT/name).read_text(encoding='utf-8'))
def digest(data):return hashlib.sha256(data).hexdigest()
def state(db):
    return json.loads(db.query("SELECT json_build_object('products',(SELECT coalesce(json_agg(t ORDER BY id),'[]') FROM product t),'gallery',(SELECT coalesce(json_agg(t ORDER BY product_id,sort_order),'[]') FROM product_image t))"))
def table_hashes(db):
    tables=json.loads(db.query("SELECT json_agg(tablename ORDER BY tablename) FROM pg_tables WHERE schemaname='public'"))
    result={}
    for table in tables:
        assert re.fullmatch('[a-z_]+',table)
        result[table]=db.query("SELECT md5(coalesce(string_agg(row_to_json(t)::text,'' ORDER BY row_to_json(t)::text),'')) FROM public."+table+' t')
    return result
def credentials():
    values={}
    source=Path(os.environ.get('BLOOMI_MEDIA_CONFIG',str(ROOT/'.local/cloudinary.properties')))
    if source.exists():
        for line in source.read_text(encoding='utf-8-sig').splitlines():
            if '=' in line and not line.lstrip().startswith('#'):
                key,value=line.split('=',1);values[key.strip()]=value.strip()
    result=[os.environ.get(env,values.get(prop,'')) for env,prop in [('CLOUDINARY_CLOUD_NAME','cloudinary.cloud-name'),('CLOUDINARY_API_KEY','cloudinary.api-key'),('CLOUDINARY_API_SECRET','cloudinary.api-secret')]]
    assert re.fullmatch('[a-zA-Z0-9_-]+',result[0]) and all(result),'Private Cloudinary configuration is incomplete'
    return result
def source_path(url):
    if not url:return None
    parsed=urllib.parse.urlparse(url)
    if parsed.scheme and parsed.hostname not in {'localhost','127.0.0.1'}:return None
    assert not parsed.query and not parsed.fragment,'Unexpected local image URL'
    if parsed.path.startswith('/products/'):
        path=(PUBLIC/urllib.parse.unquote(parsed.path).lstrip('/')).resolve()
        assert path.is_relative_to((PUBLIC/'products').resolve())
    elif parsed.path.startswith('/api/store/media/'):
        name=parsed.path.rsplit('/',1)[-1];assert re.fullmatch('[a-f0-9-]{36}\\.(png|jpg|webp)',name)
        path=ROOT/'.local/product-images'/name
    else:raise AssertionError('Unrecognized local product image path')
    assert path.is_file(),'Missing original image: '+parsed.path
    return path
def prepare():
    assert not (OUT/'plan.json').exists(),'Plan already exists; preserve and resume it'
    db=Database();db.env['PGOPTIONS']='-c default_transaction_read_only=on'
    current=state(db);collection=json.loads(COLLECTION.read_text(encoding='utf-8-sig'))
    refs={p['image_url'] for p in current['products'] if p['image_url']}
    refs.update(p['image_url'] for p in current['gallery']);refs.update(p['imageUrl'] for p in collection)
    # Include all shipped product photos so the collection is fully remote, too.
    refs.update('/'+p.relative_to(PUBLIC).as_posix() for p in (PUBLIC/'products').rglob('*') if p.is_file())
    files={};external=[]
    for url in sorted(refs):
        path=source_path(url)
        if path is None:external.append(url);continue
        data=path.read_bytes();sha=digest(data)
        assert path.suffix.lower() in {'.png','.jpg','.jpeg','.webp','.avif'},'Unsupported original format'
        files[url]={'source':path.relative_to(ROOT).as_posix(),'sha256':sha,'bytes':len(data)}
    save('plan.json',{'before':current,'tableHashes':table_hashes(db),'collectionBefore':collection,'collectionSha256':digest(COLLECTION.read_bytes()),'files':files,'externalReferences':external})
    save('uploads.json',{})
    print(json.dumps({'products':len(current['products']),'galleryEntries':len(current['gallery']),'localPaths':len(files),'uniqueFiles':len({f['sha256'] for f in files.values()}),'externalReferences':len(external)}))
def verify_url(url,expected):
    parsed=urllib.parse.urlparse(url);cloud=credentials()[0]
    assert parsed.scheme=='https' and parsed.hostname=='res.cloudinary.com' and parsed.path.startswith('/'+cloud+'/image/upload/') and not parsed.username and parsed.port is None
    with urllib.request.urlopen(url,timeout=35) as r:
        assert r.status==200 and r.headers.get('Content-Type','').startswith('image/')
        data=r.read(expected['bytes']+1)
    assert len(data)==expected['bytes'] and digest(data)==expected['sha256'],'Remote image differs from original'
def upload_one(entry):
    cloud,key,secret=credentials();data=(ROOT/entry['source']).read_bytes();assert digest(data)==entry['sha256']
    public_id='bloomibunny/products/catalog/'+entry['sha256']
    fields={'public_id':public_id,'asset_folder':'bloomibunny/products/catalog','display_name':Path(entry['source']).stem,'overwrite':'false','timestamp':str(int(time.time()))}
    signed='&'.join(k+'='+v for k,v in sorted(fields.items()))
    fields.update(api_key=key,signature=hashlib.sha256((signed+secret).encode()).hexdigest())
    boundary='bloomi-catalog-'+uuid.uuid4().hex
    body=b''.join(('--'+boundary+'\r\nContent-Disposition: form-data; name="'+k+'"\r\n\r\n'+v+'\r\n').encode() for k,v in fields.items())
    mime=mimetypes.guess_type(entry['source'])[0] or 'application/octet-stream'
    body+=('--'+boundary+'\r\nContent-Disposition: form-data; name="file"; filename="photo'+Path(entry['source']).suffix+'"\r\nContent-Type: '+mime+'\r\n\r\n').encode()+data+('\r\n--'+boundary+'--\r\n').encode()
    req=urllib.request.Request('https://api.cloudinary.com/v1_1/'+cloud+'/image/upload',data=body,headers={'Content-Type':'multipart/form-data; boundary='+boundary})
    try:
        with urllib.request.urlopen(req,timeout=60) as response:result=json.loads(response.read(65536))
    except urllib.error.HTTPError as error:raise RuntimeError('Cloudinary rejected upload, HTTP '+str(error.code)) from None
    assert result.get('public_id')==public_id and result.get('resource_type')=='image'
    url=result['secure_url'];assert len(url)<=500
    receipt={'publicId':public_id,'imageUrl':url,'sha256':entry['sha256'],'bytes':entry['bytes'],'format':result.get('format'),'verified':False}
    return receipt
def upload():
    plan=read('plan.json');receipts=read('uploads.json');unique={e['sha256']:e for e in plan['files'].values()}
    pending=[e for sha,e in unique.items() if sha not in receipts]
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        for future in concurrent.futures.as_completed([pool.submit(upload_one,e) for e in pending]):
            result=future.result();receipts[result['sha256']]=result;save('uploads.json',receipts)
            print('Uploaded',len(receipts),'/',len(unique),flush=True)
    # Save each successful receipt before verifying delivery, allowing safe resume.
    for sha,entry in unique.items():
        verify_url(receipts[sha]['imageUrl'],entry);receipts[sha]['verified']=True;save('uploads.json',receipts)
    print('All original image files uploaded and verified byte for byte.',flush=True)
def mapping(plan):
    receipts=read('uploads.json')
    result={}
    for url,entry in plan['files'].items():
        receipt=receipts[entry['sha256']];assert receipt['verified']
        result[url]=receipt['imageUrl']
    return result
def expected(plan,links):
    result=json.loads(json.dumps(plan['before']))
    gallery_products={r['product_id'] for r in result['gallery'] if r['image_url'] in links}
    for row in result['products']:
        if row['image_url'] in links or row['id'] in gallery_products:
            row['image_url']=links.get(row['image_url'],row['image_url']);row['version']+=1
    for row in result['gallery']:row['image_url']=links.get(row['image_url'],row['image_url'])
    return result
def apply():
    plan=read('plan.json');links=mapping(plan);db=Database()
    before=state(db);after=expected(plan,links)
    if before==after:
        print('Database links already migrated; finishing collection if needed.')
    else:
        assert before==plan['before'],'Products or galleries changed since planning; stop and review'
        assert digest(COLLECTION.read_bytes())==plan['collectionSha256'],'Image collection changed'
        stamp=time.strftime('%Y%m%d_%H%M%S');backup=OUT/('before-cloudinary-'+stamp+'.dump')
        subprocess.run([db.tool('pg_dump'),*db.args,'-Fc','-f',str(backup)],env=db.env,check=True,capture_output=True)
        listing=subprocess.run([db.tool('pg_restore'),'--list',str(backup)],env=db.env,check=True,capture_output=True,text=True)
        assert 'TABLE DATA public product' in listing.stdout and 'TABLE DATA public product_image' in listing.stdout
        # Exact row comparisons and locks prevent stale plans from overwriting edits.
        baseline=json.dumps(plan['before'],ensure_ascii=False)
        sql="BEGIN; SET LOCAL lock_timeout='10s'; LOCK TABLE product,product_image IN SHARE ROW EXCLUSIVE MODE;\n"
        sql+="DO $guard$ BEGIN IF (SELECT jsonb_build_object('products',(SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY id),'[]'::jsonb) FROM product t),'gallery',(SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY product_id,sort_order),'[]'::jsonb) FROM product_image t))) IS DISTINCT FROM "+quote(baseline)+"::jsonb THEN RAISE EXCEPTION 'Product media changed; migration aborted'; END IF; END $guard$;\n"
        gallery_products={r['product_id'] for r in plan['before']['gallery'] if r['image_url'] in links}
        for row in plan['before']['products']:
            if row['image_url'] in links or row['id'] in gallery_products:sql+='UPDATE product SET image_url='+quote(links.get(row['image_url'],row['image_url']))+',version=version+1 WHERE id='+str(row['id'])+';\n'
        for row in plan['before']['gallery']:
            if row['image_url'] in links:sql+='UPDATE product_image SET image_url='+quote(links[row['image_url']])+' WHERE product_id='+str(row['product_id'])+' AND sort_order='+str(row['sort_order'])+';\n'
        sql+='COMMIT;'
        db.query(sql);assert state(db)==after
        save('applied.json',{'backup':backup.name,'products':len(after['products']),'secondaryImages':len(after['gallery'])})
    collection=[{**row,'imageUrl':links.get(row['imageUrl'],row['imageUrl'])} for row in plan['collectionBefore']]
    original=json.loads(COLLECTION.read_text(encoding='utf-8-sig'))
    assert original in (plan['collectionBefore'],collection),'Image collection changed'
    if original!=collection:
        (OUT/'product-images-before.json').write_bytes(COLLECTION.read_bytes())
        COLLECTION.write_text(json.dumps(collection,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    verify()
def verify():
    plan=read('plan.json');links=mapping(plan);db=Database();db.env['PGOPTIONS']='-c default_transaction_read_only=on'
    assert state(db)==expected(plan,links),'Product/gallery mismatch'
    hashes=table_hashes(db)
    assert all(hashes[t]==h for t,h in plan['tableHashes'].items() if t not in {'product','product_image'}),'Unrelated commercial tables changed'
    collection=json.loads(COLLECTION.read_text(encoding='utf-8-sig'))
    assert collection==[{**r,'imageUrl':links.get(r['imageUrl'],r['imageUrl'])} for r in plan['collectionBefore']]
    assert all(digest((ROOT/e['source']).read_bytes())==e['sha256'] for e in plan['files'].values()),'Original files changed'
    save('verification.json',{'productFieldsPreservedExceptImageUrlAndVersion':True,'galleryOrderPreserved':True,'otherPublicTablesUnchanged':True,'originalFilesIntact':True,'imageCollectionMigrated':True,'uniqueImages':len({e['sha256'] for e in plan['files'].values()}),'localPathsMigrated':len(links)})
    print('Migration verified: photos, gallery order, originals and commercial data preserved.')
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('action',choices=['prepare','upload','apply','verify']);args=parser.parse_args()
    globals()[args.action]()
