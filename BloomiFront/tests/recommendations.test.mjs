import assert from 'node:assert/strict';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { buildSync } from 'esbuild';

// Use Vite's installed compiler; no additional testing library or dependency.
const bundle = buildSync({ entryPoints:[fileURLToPath(new URL('../src/services/recommendations.ts',import.meta.url))], bundle:true, platform:'node', format:'esm', write:false });
const { recommendProducts } = await import('data:text/javascript;base64,'+Buffer.from(bundle.outputFiles[0].text).toString('base64'));
const product = (id,name,category='Labios',extra={}) => ({id,name,category,active:true,stock:5,salePrice:15000,imageUrl:`/products/${id}.png`,...extra});
const ids = entries => entries.map(entry=>entry.product.id);

test('same type and category outrank lip complements and neighboring categories',()=>{
 const current=product(1,'Gloss de prueba');
 const items=[current,product(2,'Kit Gloss'),product(3,'Tinta labial'),product(4,'Rubor','Rostro')];
 assert.deepEqual(ids(recommendProducts(current,items)),[2,3,4]);
});
test('palettes recommend eyes first, then blush and highlighter, without unrelated skincare',()=>{
 const current=product(1,'Paleta de Sombras','Ojos');
 const items=[current,product(2,'Sombra cálida','Ojos'),product(3,'Rubor doble','Rostro'),product(4,'Iluminador','Rostro'),product(5,'Sérum','Cuidado de piel')];
 assert.deepEqual(ids(recommendProducts(current,items)),[2,3,4]);
});
test('explicit Toy Story collection contributes to otherwise equal relevance',()=>{
 const current=product(1,'Labial Doble Vaquero Toy Story');
 const items=[product(2,'Tinta convencional'),product(3,'Tintas Vaqueras Toy Story'),product(4,'Cartuchera Toy Story','Accesorios'),product(5,'Storybook','Desconocida')];
 assert.deepEqual(ids(recommendProducts(current,items)),[3,2,4]);
});
test('Lotso has a recognized collection, without arbitrary partial-name matches',()=>{
 const current=product(1,'Accesorios Pelo Lotso','Accesorios');
 const items=[product(2,'Monedero Lotso','Rostro'),product(3,'Flor Lotsomething','Desconocida')];
 assert.deepEqual(ids(recommendProducts(current,items)),[2]);
});
test('never recommends the current SKU or another tone of the current family',()=>{
 const current=product(1,'Labial Toy Story','Labios',{variantGroup:'toy-labial',shade:'Tono 2'});
 const items=[current,product(2,'Labial Toy Story','Labios',{variantGroup:'toy-labial',shade:'Tono 3'}),product(3,'Gloss')];
 assert.deepEqual(ids(recommendProducts(current,items)),[3]);
});
test('duplicate SKUs and tone families produce one card, retaining published tone information',()=>{
 const current=product(1,'Gloss');
 const two=product(2,'Tinta','Labios',{variantGroup:'tintas',shade:'Tono 2',stock:0});
 const three=product(3,'Tinta','Labios',{variantGroup:'tintas',shade:'Tono 3'});
 const result=recommendProducts(current,[two,three,three]);
 assert.equal(result.length,1);assert.equal(result[0].product.id,3);assert.equal(result[0].variants.length,2);
});
test('inactive and sold-out product families are absent',()=>{
 const current=product(1,'Gloss');
 const result=recommendProducts(current,[product(2,'Gloss oculto','Labios',{active:false}),product(3,'Gloss agotado','Labios',{stock:0}),product(4,'Gloss disponible')]);
 assert.deepEqual(ids(result),[4]);
});
test('returns fewer relevant options or no options instead of arbitrary filler',()=>{
 const current=product(1,'Sérum facial','Cuidado de piel');
 assert.deepEqual(ids(recommendProducts(current,[product(2,'Stickers para Acné','Cuidado de piel'),product(3,'Sombras','Ojos')])),[2]);
 assert.deepEqual(recommendProducts(current,[product(3,'Sombras','Ojos')]),[]);
});
test('ranking and tone representatives stay stable when API rows are reordered',()=>{
 const current=product(1,'Gloss');
 const rows=[product(7,'Gloss B'),product(4,'Gloss A'),product(3,'Gloss C','Labios',{variantGroup:'glosses',shade:'Rosa'}),product(2,'Gloss C','Labios',{variantGroup:'glosses',shade:'Rosa'})];
 assert.deepEqual(ids(recommendProducts(current,rows)),[2,4,7]);
 assert.deepEqual(recommendProducts(current,rows),recommendProducts(current,[...rows].reverse()));
});
test('keeps real prices and covers, caps the list at six, and leaves input untouched',()=>{
 const current=product(1,'Gloss');
 const rows=Array.from({length:10},(_,i)=>product(i+2,'Gloss '+i,'Labios',{salePrice:12000.25+i}));
 const before=structuredClone(rows);const result=recommendProducts(current,rows,100);
 assert.equal(result.length,6);assert.deepEqual(rows,before);assert.equal(result[0].product.salePrice,12000.25);assert.equal(result[0].imageUrl,'/products/2.png');assert.deepEqual(recommendProducts(current,rows,0),[]);
});
