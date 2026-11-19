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
test('keeps real prices and covers, caps the list at ten, and leaves input untouched',()=>{
 const current=product(1,'Gloss');
 const rows=Array.from({length:10},(_,i)=>product(i+2,'Gloss '+i,'Labios',{salePrice:12000.25+i}));
 const before=structuredClone(rows);const result=recommendProducts(current,rows,100);
 assert.equal(result.length,10);assert.deepEqual(rows,before);assert.equal(result[0].product.salePrice,12000.25);assert.equal(result[0].imageUrl,'/products/2.png');assert.deepEqual(recommendProducts(current,rows,0),[]);
});

test('a large lip category leaves room for three neighboring categories in ten results',()=>{
 const current=product(1,'Gloss actual');
 const lips=Array.from({length:14},(_,i)=>product(i+2,'Gloss '+i));
 const nearby=[product(30,'Rubor suave','Rubores'),product(31,'Paleta de sombras','Sombras'),product(32,'Base de maquillaje','Bases')];
 const result=recommendProducts(current,[...lips,...nearby,product(40,'Monedero','Accesorios')]);
 assert.equal(result.length,10);assert.equal(result.filter(e=>e.product.category==='Labios').length,7);
 assert.deepEqual(ids(result).slice(-3),[30,31,32]);assert.equal(ids(result).includes(40),false);
 assert.deepEqual(result,recommendProducts(current,[...nearby,...lips].reverse()));
});
test('eyes and lips can recommend each other even without matching product types',()=>{
 assert.deepEqual(ids(recommendProducts(product(1,'Delineador','Delineadores'),[product(2,'Gloss','Labiales')])) ,[2]);
 assert.deepEqual(ids(recommendProducts(product(1,'Gloss','Labiales'),[product(2,'Delineador','Delineadores')])) ,[2]);
});
test('facial care and face makeup relate while eye makeup and generic accessories stay unrelated to care',()=>{
 const current=product(1,'Sérum','Skincare');
 assert.deepEqual(ids(recommendProducts(current,[product(2,'Stickers para acné','Cuidado facial'),product(3,'Base','Bases'),product(4,'Paleta','Sombras'),product(5,'Cartuchera','Accesorios')])),[2,3]);
});
test('tools relate to face and eye categories, without unrelated lip or hair padding',()=>{
 const current=product(1,'Brocha','Brochas');
 assert.deepEqual(ids(recommendProducts(current,[product(2,'Base','Bases'),product(3,'Delineador','Delineadores'),product(4,'Gloss','Labios'),product(5,'Kit pelo','Accesorios')])),[2,3]);
});
test('limited and unavailable catalogs never create fake entries to reach ten',()=>{
 const current=product(1,'Gloss');
 const rows=[product(2,'Rubor','Rostro'),product(3,'Paleta','Ojos',{stock:0}),product(4,'Tinta','Labios',{active:false})];
 assert.deepEqual(ids(recommendProducts(current,rows)),[2]);
 assert.deepEqual(recommendProducts(current,rows,0),[]);assert.deepEqual(recommendProducts(current,rows,-1),[]);
});

test('sweaters and cardigans recommend each other without mixing in makeup',()=>{
 const sweater=product(29,'Suéter Eternal Time','Suéter');
 const cardigan=product(26,'Cárdigan Black & White','Cárdigan');
 const otherSweater=product(28,'Suéter Mystic Skull','Suéter');
 const otherCardigan=product(27,'Cárdigan Gothic Skull','Cárdigan');
 const makeup=product(30,'Gloss Corazón','Labios');
 assert.deepEqual(ids(recommendProducts(sweater,[sweater,cardigan,otherSweater,otherCardigan,makeup])),[28,26,27]);
 assert.deepEqual(ids(recommendProducts(cardigan,[sweater,cardigan,otherSweater,otherCardigan,makeup])),[27,28,29]);
});
test('knitwear category aliases and names work across accents and plural forms',()=>{
 for(const category of ['Suéteres','Sueter','Sweaters']){
  const result=recommendProducts(product(1,'Prenda A',category),[product(2,'Prenda B','Cárdiganes'),product(3,'Prenda C','Cardigans')]);
  assert.deepEqual(ids(result),[2,3]);
 }
 assert.deepEqual(ids(recommendProducts(product(1,'Sweater gris','Prendas'),[product(2,'Cardigan negro','Ropa')])),[2]);
});
