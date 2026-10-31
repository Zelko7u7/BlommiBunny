import assert from 'node:assert/strict';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { buildSync } from 'esbuild';

const bundle = buildSync({ entryPoints: [fileURLToPath(new URL('../src/services/heroNavigation.ts', import.meta.url))], bundle: true, platform: 'node', format: 'esm', write: false });
const { heroButtonHref } = await import('data:text/javascript;base64,' + Buffer.from(bundle.outputFiles[0].text).toString('base64'));
const lips = { type: 'category', category: 'Labios', aliases: ['Labiales'], fallbackQuery: 'labios' };

test('collection keeps the current catalogue and navigates to its native fragment', () => {
  assert.equal(heroButtonHref({ type: 'catalog' }, ['Labios'], '?q=gloss'), '#catalogo');
});
test('lips uses the real category and clears an unrelated query/category', () => {
  const href = heroButtonHref(lips, ['Ojos', 'Labios', 'Rostro'], '?q=sombra&category=Ojos&sort=price-desc');
  const url = new URL(href, 'https://example.com/');
  assert.equal(url.searchParams.get('category'), 'Labios');
  assert.equal(url.searchParams.has('q'), false); assert.equal(url.searchParams.get('sort'), 'price-desc'); assert.equal(url.hash, '#catalogo');
});
test('uses a real alias without inventing a category value', () => {
  const url = new URL(heroButtonHref(lips, ['LABIALES', 'Ojos'], ''), 'https://example.com/');
  assert.equal(url.searchParams.get('category'), 'LABIALES');
});
test('falls back to the existing search when no equivalent category exists', () => {
  const url = new URL(heroButtonHref(lips, ['Ojos'], '?q=sombra&category=Ojos'), 'https://example.com/');
  assert.equal(url.searchParams.get('q'), 'labios'); assert.equal(url.searchParams.has('category'), false);
});
test('normalization matches only full category labels and preserves encoding', () => {
  const action = { type: 'category', category: 'Cuidado de piel', fallbackQuery: 'piel' };
  const url = new URL(heroButtonHref(action, [' CUIDADO DE PIEL ', 'Cuidado corporal'], ''), 'https://example.com/');
  assert.equal(url.searchParams.get('category'), ' CUIDADO DE PIEL ');
  assert.equal(new URL(heroButtonHref(lips, ['Accesorios para labios'], ''), 'https://example.com/').searchParams.has('category'), false);
});
