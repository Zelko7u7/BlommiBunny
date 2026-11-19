import assert from 'node:assert/strict';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { buildSync } from 'esbuild';
const bundle=buildSync({entryPoints:[fileURLToPath(new URL('../src/services/imageUploads.ts',import.meta.url))],bundle:true,platform:'node',format:'esm',write:false});
const {validateImageFile,uniqueGallery,MAX_IMAGE_BYTES,MAX_PRODUCT_IMAGES}=await import('data:text/javascript;base64,'+Buffer.from(bundle.outputFiles[0].text).toString('base64'));
test('supported photos accept the documented size boundary',()=>{
 for(const type of ['image/jpeg','image/png','image/webp'])assert.equal(validateImageFile({type,size:MAX_IMAGE_BYTES}),undefined);
 assert.equal(MAX_PRODUCT_IMAGES,21);
});
test('empty, oversized and unsupported files are rejected before upload',()=>{
 for(const file of [{type:'image/png',size:0},{type:'image/jpeg',size:MAX_IMAGE_BYTES+1},{type:'image/svg+xml',size:10},{type:'text/html',size:10}])assert.ok(validateImageFile(file));
});
test('gallery keeps order and removes duplicates and cover without changing the input',()=>{
 const gallery=[' /products/a.png ','/products/cover.png','/products/a.png','','https://example.com/b.webp'];const before=[...gallery];
 assert.deepEqual(uniqueGallery('/products/cover.png',gallery),['/products/a.png','https://example.com/b.webp']);assert.deepEqual(gallery,before);
});
