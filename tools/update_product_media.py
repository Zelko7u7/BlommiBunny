"""Prepare and apply the owner's official product media without changing inventory.

python tools/update_product_media.py --prepare
python tools/update_product_media.py --apply
python tools/update_product_media.py --verify

Requires the supplied Markdown, Productos/ originals, local backend configuration,
and PostgreSQL tools. No Python dependencies. Private backups/plans stay in .local/.
"""
import argparse
from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import unicodedata

ROOT = Path(__file__).resolve().parents[1]
PRIVATE = ROOT / '.local/product-media-2026-10-28'
PUBLIC = ROOT / 'BloomiFront/public'
SOURCE = ROOT / 'Productos'
DOCUMENT = ROOT / 'BloomiBunny_descripciones_y_mapeo.md'
TABLES = ('product','sale','sale_item','purchase','purchase_item','expense','inventory_movement','favorite','category','brand','supplier','customer','app_user')

def normalized(value):
    return re.sub(r'[^a-z0-9]', '', unicodedata.normalize('NFD', value).encode('ascii','ignore').decode().lower())

def slug(value):
    return re.sub(r'[^a-z0-9]+','-',unicodedata.normalize('NFD',value).encode('ascii','ignore').decode().lower()).strip('-')

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def read_descriptions(text):
    result = {}
    for match in re.finditer(r'^## ([^\n]+)\n(.*?)(?=^## |\Z)', text.replace('\r\n','\n'), re.M|re.S):
        heading, body = match.groups()
        # Metadata is not part of the owner's product prose.
        paragraphs = [p for p in body.strip().split('\n\n') if not p.startswith(('**Archivos ZIP:**','**Tonos disponibles'))]
        description = '\n\n'.join(paragraphs)
        if not description or len(description)>1200:
            raise ValueError('Descripción vacía o demasiado larga: '+heading)
        result[heading] = description
    if len(result)!=21:
        raise ValueError('Se esperaban exactamente 21 descripciones oficiales.')
    return result

def extension(path):
    header = path.read_bytes()[:32]
    if header.startswith(b'\x89PNG\r\n\x1a\n'): return '.png'
    if header.startswith(b'\xff\xd8\xff'): return '.jpg'
    if header[:4]==b'RIFF' and header[8:12]==b'WEBP': return '.webp'
    if header[4:8]==b'ftyp' and b'avif' in header: return '.avif'
    raise ValueError('Formato no reconocido: '+path.name)

class Database:
    def __init__(self):
        settings={}
        for line in (ROOT/'BloomiBack/src/main/resources/application.properties').read_text(encoding='utf-8').splitlines():
            if '=' in line and not line.lstrip().startswith('#'):
                key,value=line.split('=',1)
                settings[key]=re.sub(r'\$\{([^:}]+)(?::([^}]*))?\}',lambda m:os.environ.get(m[1],m[2] or ''),value)
        match=re.fullmatch(r'jdbc:postgresql://([^:/]+):(\d+)/([^/?]+)',settings['spring.datasource.url'])
        if not match: raise ValueError('Revisa la URL PostgreSQL local antes de continuar.')
        host,port,name=match.groups()
        self.args=['-h',host,'-p',port,'-U',settings['spring.datasource.username'],'-d',name]
        self.env={**os.environ,'PGPASSWORD':settings['spring.datasource.password'],'PGCLIENTENCODING':'UTF8'}
    def tool(self,name):
        return shutil.which(name) or str(Path('C:/Program Files/PostgreSQL/18/bin')/(name+'.exe'))
    def query(self,sql):
        result=subprocess.run([self.tool('psql'),*self.args,'-X','-v','ON_ERROR_STOP=1','-At'],input=sql,env=self.env,capture_output=True,text=True,encoding='utf-8',check=True)
        return result.stdout.strip()
    def snapshot(self):
        sql="SELECT json_build_object("+','.join(quote(t)+",(SELECT coalesce(json_agg(row_to_json(p) ORDER BY id),'[]') FROM public."+t+' p)' for t in TABLES)+");"
        return json.loads(self.query(sql))
    def backup(self,stamp):
        path=PRIVATE/f'before-media-{stamp}.dump'
        subprocess.run([self.tool('pg_dump'),*self.args,'-Fc','-f',str(path)],env=self.env,check=True,capture_output=True)
        listing=subprocess.run([self.tool('pg_restore'),'--list',str(path)],env=self.env,capture_output=True,text=True,check=True)
        if 'TABLE DATA public product' not in listing.stdout: raise ValueError('El respaldo no contiene productos.')
        return path

def quote(value):
    return 'NULL' if value is None else "'"+str(value).replace("'","''")+"'"

def json_write(path,value):
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2),encoding='utf-8')

def prepare(db):
    descriptions=read_descriptions(DOCUMENT.read_text(encoding='utf-8'))
    mapping=json.loads((ROOT/'tools/product_media_map.json').read_text(encoding='utf-8'))
    before=db.snapshot()
    by_code={p['code']:p for p in before['product']}
    plan=[];used=set();duplicates=[];hashes={}
    for entry in mapping:
        description=descriptions[entry['heading']]
        for match in entry['products']:
            row=by_code.get(match['code'])
            if not row or row['deleted'] or normalized(row['name'])!=normalized(match['name']) or row['shade']!=match.get('shade'):
                raise ValueError('Correspondencia no confirmada: '+match['code'])
            prefix=match.get('sourcePrefix',entry.get('sourcePrefix'))
            photos=[]
            for path in SOURCE.iterdir():
                identified=re.fullmatch(re.escape(prefix)+r' (\d+)\.(?:png|jpg)',path.name,re.I)
                if identified: photos.append((int(identified[1]),path))
            photos.sort()
            if not photos: raise ValueError('No se encontraron fotografías: '+prefix)
            cover=row['image_url']
            if cover:
                if not cover.startswith('/products/') or not (PUBLIC/cover.lstrip('/')).is_file():
                    raise ValueError('Portada existente requiere revisión: '+match['code'])
                hashes[cover]=digest(PUBLIC/cover.lstrip('/'))
            unique=[];seen=set()
            if cover: seen.add(digest(PUBLIC/cover.lstrip('/')))
            for number,path in photos:
                if path.name in used: raise ValueError('Fotografía asociada a dos SKU: '+path.name)
                used.add(path.name)
                sha=digest(path)
                if sha in seen:
                    duplicates.append({'code':row['code'],'file':path.name,'reason':'Contenido idéntico; se muestra una sola vez'})
                    continue
                seen.add(sha)
                url='/products/gallery/'+slug(prefix)+f'-{number:02d}'+extension(path)
                target=PUBLIC/url.lstrip('/')
                target.parent.mkdir(parents=True,exist_ok=True)
                if target.exists() and digest(target)!=sha: raise ValueError('Archivo publicado distinto: '+url)
                if not target.exists(): shutil.copyfile(path,target)
                hashes[url]=sha
                unique.append(url)
            if not cover:
                cover=unique.pop(0)
            # Preserve any pre-existing secondary photos on subsequent imports.
            secondary=[]
            if db.query("SELECT to_regclass('public.product_image') IS NOT NULL")== 't':
                secondary=json.loads(db.query('SELECT coalesce(json_agg(image_url ORDER BY sort_order),\'[]\') FROM public.product_image WHERE product_id='+str(row['id'])))
            gallery=list(dict.fromkeys(url for url in [*secondary,*unique] if url!=cover))
            if len(gallery)>20: raise ValueError('Demasiadas fotografías: '+row['code'])
            plan.append({'id':row['id'],'code':row['code'],'name':row['name'],'shade':row['shade'],'active':row['active'],'description':description,'imageUrl':cover,'galleryImages':gallery,'previous':row,'sourceHeading':entry['heading']})
    unmatched=sorted(p.name for p in SOURCE.iterdir() if p.is_file() and p.name not in used)
    if unmatched: raise ValueError('Fotografías sin asociación: '+', '.join(unmatched))
    payload={'sourceSha256':digest(DOCUMENT),'products':plan,'assetHashes':hashes,'duplicates':duplicates,'unmatchedProducts':[{'id':p['id'],'name':p['name']} for p in before['product'] if p['id'] not in {v['id'] for v in plan}]}
    json_write(PRIVATE/'prepared-plan.json',payload)
    return payload,before

def verify(db,plan,before=None):
    current=db.snapshot()
    rows={p['id']:p for p in current['product']}
    for item in plan['products']:
        p=rows[item['id']]
        assert p['description']==item['description'] and p['image_url']==item['imageUrl'],item['code']
        gallery=json.loads(db.query('SELECT coalesce(json_agg(image_url ORDER BY sort_order),\'[]\') FROM public.product_image WHERE product_id='+str(p['id'])))
        assert gallery==item['galleryImages'],item['code']
    for url,sha in plan['assetHashes'].items(): assert digest(PUBLIC/url.lstrip('/'))==sha,url
    if before:
        assert len(before['product'])==len(current['product'])
        for table in TABLES:
            if table!='product': assert before[table]==current[table],table
        changed={p['id'] for p in plan['products']}
        allowed={'description','image_url','updated_at','version'}
        for p in before['product']:
            now=rows[p['id']]
            assert all(now[k]==v for k,v in p.items() if not(p['id'] in changed and k in allowed)),p['code']
    return current

def apply(db,plan,before):
    stamp=datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    json_write(PRIVATE/f'before-apply-{stamp}.json',before)
    backup=db.backup(stamp)
    migration=(ROOT/'BloomiBack/src/main/resources/db/migrations/009_product_gallery.sql').read_text(encoding='utf-8')
    migration=re.sub(r'^BEGIN;|^COMMIT;','',migration,flags=re.M)
    statements=['BEGIN; SET LOCAL lock_timeout=\'10s\';',migration]
    for item in sorted(plan['products'],key=lambda p:p['id']):
        statements.append('SELECT id FROM public.product WHERE id='+str(item['id'])+' FOR UPDATE;')
    for item in plan['products']:
        old=json.dumps(item['previous'],ensure_ascii=False)
        statements.append('DO $media$ BEGIN IF (SELECT to_jsonb(p) FROM public.product p WHERE id='+str(item['id'])+') IS DISTINCT FROM '+quote(old)+'::jsonb THEN RAISE EXCEPTION \'El producto cambió desde la preparación; no se sobrescribe.\'; END IF; END $media$;')
        expected_gallery=quote(json.dumps(item['galleryImages'],ensure_ascii=False))+'::jsonb'
        gallery_changed="(SELECT coalesce(json_agg(image_url ORDER BY sort_order)::jsonb,'[]'::jsonb) FROM public.product_image WHERE product_id="+str(item['id'])+') IS DISTINCT FROM '+expected_gallery
        statements.append('UPDATE public.product SET description='+quote(item['description'])+',image_url='+quote(item['imageUrl'])+',updated_at=CURRENT_TIMESTAMP,version=version+1 WHERE id='+str(item['id'])+' AND (description IS DISTINCT FROM '+quote(item['description'])+' OR image_url IS DISTINCT FROM '+quote(item['imageUrl'])+' OR '+gallery_changed+');')
        # Insert only missing photos; no deletions or reorder of previous data.
        for order,url in enumerate(item['galleryImages']):
            statements.append('INSERT INTO public.product_image(product_id,sort_order,image_url) SELECT '+str(item['id'])+','+str(order)+','+quote(url)+' WHERE NOT EXISTS(SELECT 1 FROM public.product_image WHERE product_id='+str(item['id'])+' AND image_url='+quote(url)+');')
    statements.append('COMMIT;')
    db.query('\n'.join(statements))
    current=verify(db,plan,before)
    json_write(PRIVATE/'commercial-after.json',current)
    json_write(PRIVATE/'applied-plan.json',plan)
    print('Actualización atómica aplicada. Respaldo privado:',backup.relative_to(ROOT))

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    group=parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--prepare',action='store_true');group.add_argument('--apply',action='store_true');group.add_argument('--verify',action='store_true')
    options=parser.parse_args();PRIVATE.mkdir(parents=True,exist_ok=True);db=Database()
    if options.verify:
        plan=json.loads((PRIVATE/'applied-plan.json').read_text(encoding='utf-8'))
        before=json.loads((PRIVATE/'commercial-before.json').read_text(encoding='utf-8')) if (PRIVATE/'commercial-before.json').exists() else None
        verify(db,plan,before);print('Descripciones, galerías, portadas y datos comerciales verificados.');return
    plan,before=prepare(db)
    print(json.dumps({'descriptions':len(plan['products']),'newCovers':sum(not p['previous']['image_url'] for p in plan['products']),'secondaryPhotos':sum(len(p['galleryImages']) for p in plan['products']),'publishedAssets':len(plan['assetHashes']),'duplicates':plan['duplicates'],'pending':plan['unmatchedProducts']},ensure_ascii=False))
    if options.apply:apply(db,plan,before)

if __name__=='__main__':
    main()
