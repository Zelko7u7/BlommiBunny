"""Seed the two former static banners once, preserving all originals.

Apply 013_promotional_banners.sql with a verified backup first. Then run:
python tools/migrate_promotional_banners.py --lips-category-id CONFIRMED_ID
The category ID is an installation argument verified by the operator; the UI
uses selectors. A separate seed marker prevents deleted promotions returning.
"""
from pathlib import Path
import argparse,hashlib,json,time,urllib.request,urllib.parse,uuid
from update_product_media import Database,quote
from migrate_product_images_cloudinary import credentials
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'.local/banner-seed'
def migrate(category_id):
 db=Database()
 if db.query("SELECT count(*) FROM promotional_banner_seed WHERE seed_key='initial-static-v1'")=='1':
  print('Initial banners already migrated; existing edits, deactivations and deletions preserved.');return
 assert db.query('SELECT count(*) FROM category WHERE active AND id='+str(category_id))=='1','Confirm an existing active lip category first'
 OUT.mkdir(parents=True,exist_ok=True)
 manifest=OUT/'uploads.json';receipts=json.loads(manifest.read_text(encoding='utf-8')) if manifest.exists() else {}
 cloud,key,secret=credentials()
 definitions=[('welcome','banner-bienvenida.webp','Bienvenida','Tu belleza florece aquí.','Descubre pequeños detalles para brillar cada día.','CATALOG',None),('lips','banner-labios.webp','Productos para labios','Un toque de brillo para ti.','Glosses, tintas y bálsamos para cada momento.','CATEGORY',category_id)]
 for name,file,title,headline,description,kind,ref in definitions:
  data=(ROOT/'BloomiFront/public/banners'/file).read_bytes();sha=hashlib.sha256(data).hexdigest()
  if name not in receipts:
   public_id='bloomi-bunny/banners/initial-'+name+'-'+sha[:16]
   fields={'public_id':public_id,'asset_folder':'bloomi-bunny/banners','display_name':title,'overwrite':'false','timestamp':str(int(time.time()))}
   signature=hashlib.sha256(('&'.join(k+'='+v for k,v in sorted(fields.items()))+secret).encode()).hexdigest();fields.update(api_key=key,signature=signature)
   boundary='bloomi-banner-seed-'+uuid.uuid4().hex
   body=b''.join(('--'+boundary+'\r\nContent-Disposition: form-data; name="'+k+'"\r\n\r\n'+v+'\r\n').encode() for k,v in fields.items())
   body+=('--'+boundary+'\r\nContent-Disposition: form-data; name="file"; filename="banner.webp"\r\nContent-Type: image/webp\r\n\r\n').encode()+data+('\r\n--'+boundary+'--\r\n').encode()
   request=urllib.request.Request('https://api.cloudinary.com/v1_1/'+cloud+'/image/upload',data=body,headers={'Content-Type':'multipart/form-data; boundary='+boundary})
   with urllib.request.urlopen(request,timeout=35) as response:result=json.loads(response.read(65536))
   assert result['public_id']==public_id
   receipts[name]={'url':result['secure_url'],'publicId':public_id,'width':result['width'],'height':result['height'],'sha256':sha}
   manifest.write_text(json.dumps(receipts,ensure_ascii=False,indent=2),encoding='utf-8')
  image=receipts[name];url=urllib.parse.urlparse(image['url']);assert url.scheme=='https' and url.hostname=='res.cloudinary.com' and url.path.startswith('/'+cloud+'/image/upload/')
  with urllib.request.urlopen(image['url'],timeout=25) as response:assert hashlib.sha256(response.read()).hexdigest()==sha
  print('Original banner uploaded and verified:',title,flush=True)
 sql="BEGIN; SELECT pg_advisory_xact_lock(721604119);\n"
 for order,(name,file,title,headline,description,kind,ref) in enumerate(definitions):
  image=receipts[name]
  values=[title,image['url'],image['publicId'],image['width'],image['height'],title,headline,description,kind,ref,order,str(uuid.uuid5(uuid.NAMESPACE_URL,'bloomi-initial-banner-'+name)),name+'-v1']
  sql+='INSERT INTO promotional_banner(internal_name,desktop_image_url,desktop_image_public_id,desktop_width,desktop_height,alt_text,headline,description,target_type,category_id,sort_order,request_key,legacy_key,active) SELECT '+','.join(quote(v) for v in values)+",true WHERE NOT EXISTS(SELECT 1 FROM promotional_banner_seed WHERE seed_key='initial-static-v1') ON CONFLICT(legacy_key) DO NOTHING;\n"
 sql+="INSERT INTO promotional_banner_seed(seed_key) VALUES('initial-static-v1') ON CONFLICT DO NOTHING; COMMIT;"
 db.query(sql)
 print('Initial banners migrated once; category linked by stable ID. Originals retained.')
if __name__=='__main__':
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--lips-category-id',required=True,type=int);migrate(parser.parse_args().lips_category_id)
