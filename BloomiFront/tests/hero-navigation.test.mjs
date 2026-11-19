import assert from 'node:assert/strict';
import test from 'node:test';
import {fileURLToPath} from 'node:url';
import {buildSync} from 'esbuild';
const bundle=buildSync({entryPoints:[fileURLToPath(new URL('../src/services/heroNavigation.ts',import.meta.url))],bundle:true,platform:'node',format:'esm',write:false});
const {heroBannerHref}=await import('data:text/javascript;base64,'+Buffer.from(bundle.outputFiles[0].text).toString('base64'));
test('full catalog clears previous category and search but preserves valid price ordering',()=>{
 assert.equal(heroBannerHref('/#catalogo','?q=sombra&category=Ojos&categoryId=2&sort=price-desc'),'/?sort=price-desc#catalogo');
});
test('category destination keeps its stable ID and removes incompatible refinements',()=>{
 const url=new URL(heroBannerHref('/?categoryId=7#catalogo','?q=sombra&category=Ojos&sort=price-asc'),'https://example.com');
 assert.equal(url.searchParams.get('categoryId'),'7');assert.equal(url.searchParams.get('sort'),'price-asc');assert.equal(url.searchParams.has('q'),false);assert.equal(url.searchParams.has('category'),false);assert.equal(url.hash,'#catalogo');
});
test('product and existing sections stay native routes',()=>{for(const href of ['/producto/12','/nosotros','/favoritos'])assert.equal(heroBannerHref(href,'?q=gloss'),href);});
test('external links and unknown or injected routes cannot redirect buyers',()=>{for(const href of ['https://evil.invalid','//evil.invalid','javascript:alert(1)','/producto/0','/?categoryId=2&next=https://evil.invalid#catalogo','/admin','/politicas/falsa'])assert.equal(heroBannerHref(href),'/#catalogo');});
test('invalid previous sort never enters a destination',()=>assert.equal(heroBannerHref('/?categoryId=1#catalogo','?sort=javascript:evil'),'/?categoryId=1#catalogo'));
test('explicit alphabetical sorting remains selected when navigating through a banner',()=>assert.equal(heroBannerHref('/?categoryId=1#catalogo','?sort=name'),'/?categoryId=1&sort=name#catalogo'));
