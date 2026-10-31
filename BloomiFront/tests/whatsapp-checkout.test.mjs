import assert from 'node:assert/strict';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { buildSync } from 'esbuild';

async function load(path) {
  const bundle = buildSync({ entryPoints: [fileURLToPath(new URL(path, import.meta.url))], bundle: true, platform: 'node', format: 'esm', write: false });
  return import('data:text/javascript;base64,' + Buffer.from(bundle.outputFiles[0].text).toString('base64'));
}
const { reviewWhatsAppCart, buildWhatsAppMessage, prepareWhatsAppCheckout, validWhatsAppNumber, openWhatsAppCheckout, MAX_WHATSAPP_URL_LENGTH } = await load('../src/services/whatsappCheckout.ts');
const { readCart, changeQuantity, cartLines } = await load('../src/services/cart.ts');
const number = '573242213841'; // Public, confirmed business number; tests never contact it.
const product = (id, extra = {}) => ({ id, name: `Producto ${id}`, category: 'Labios', active: true, stock: 10, salePrice: 15000, ...extra });
const item = (id, quantity = 1) => ({ productId: id, quantity });
const prepare = (cart, products) => prepareWhatsAppCheckout(cart, products, number);

test('single non-variant SKU includes price and quantity but no invented shade', () => {
  const order = prepare([item(1, 2)], [product(1, { name: 'Lip Balm Ham #03' })]);
  assert.match(order.message, /1\. Lip Balm Ham #03/); assert.doesNotMatch(order.message, /Tono:/);
  assert.match(order.message, /Cantidad: 2/); assert.match(order.message, /Precio unitario: \$15\.000 COP/); assert.match(order.message, /Subtotal: \$30\.000 COP/);
  assert.equal(order.units, 2); assert.equal(order.subtotalCents, 3000000);
});
test('persisted selected shade is included without deriving it from a name', () => {
  const order = prepare([item(11)], [product(11, { name: 'Labial Doble Vaquero Toy Story', shade: 'Tono 2', variantGroup: 'labial-doble-vaquero-toy-story' })]);
  assert.match(order.message, /Tono: Tono 2/); assert.equal(order.lines[0].productId, 11);
});
test('two shades of one family remain distinct SKU lines and prices', () => {
  const products = [product(11, { name: 'Labial Vaquero', shade: 'Tono 2', variantGroup: 'vaquero', salePrice: 15000 }), product(12, { name: 'Labial Vaquero', shade: 'Tono 3', variantGroup: 'vaquero', salePrice: 18000 })];
  const order = prepare([item(11), item(12, 2)], products);
  assert.deepEqual(order.lines.map(line => line.productId), [11, 12]); assert.equal(order.units, 3); assert.equal(order.subtotalCents, 5100000);
  assert.match(order.message, /1\. Labial Vaquero\n   Tono: Tono 2/); assert.match(order.message, /2\. Labial Vaquero\n   Tono: Tono 3/);
});
test('three or more products preserve every requested line and total', () => {
  const order = prepare([item(1), item(2, 2), item(3, 3), item(4)], [product(1), product(2), product(3), product(4)]);
  assert.equal(order.lines.length, 4); assert.equal(order.units, 7); assert.match(order.message, /TOTAL DE PRODUCTOS: 7/); assert.match(order.message, /SUBTOTAL: \$105\.000 COP/);
});
test('decimal prices are added as cents without floating-point total errors', () => {
  const order = prepare([item(1, 3), item(2)], [product(1, { salePrice: 0.1 }), product(2, { salePrice: 10.25 })]);
  assert.equal(order.subtotalCents, 1055); assert.match(order.message, /SUBTOTAL: \$10,55 COP/);
});
test('zero catalog price is preserved rather than replaced with a promotion', () => {
  const order = prepare([item(1)], [product(1, { salePrice: 0 })]); assert.match(order.message, /Precio unitario: \$0 COP/); assert.equal(order.subtotalCents, 0);
});
test('empty cart cannot create any message or link', () => {
  assert.throws(() => prepare([], [])); assert.equal(reviewWhatsAppCart([], []).issues.length, 1);
});
for (const [label, products] of [
  ['out of stock', [product(1, { stock: 0 })]], ['disabled', [product(1, { active: false })]], ['deleted or missing', []],
]) test(`${label} blocks the complete request instead of silently removing an item`, () => {
  const cart = [item(1), item(2)]; const catalog = [...products, product(2)];
  assert.throws(() => prepare(cart, catalog)); assert.ok(reviewWhatsAppCart(cart, catalog).issues.some(issue => issue.productId === 1));
});
test('quantity above known stock is rejected rather than clamped in the message', () => {
  const cart = [item(1, 3)]; const products = [product(1, { stock: 2 })];
  assert.equal(cartLines(cart, products)[0].quantity, 2); assert.throws(() => prepare(cart, products)); assert.equal(cart[0].quantity, 3);
});
test('invalid IDs, duplicate IDs and quantities never produce a request', () => {
  for (const cart of [[item(0)], [item(-1)], [item('1')], [item(1.2)], [item(1), item(1)], [item(1, 0)], [item(1, -1)], [item(1, 1.2)], [item(1, NaN)], [item(1, Infinity)]]) assert.throws(() => prepare(cart, [product(1)]));
});
test('invalid prices, stock and missing commercial name block checkout', () => {
  for (const salePrice of [NaN, Infinity, -1, 1.001, '15000', Number.MAX_SAFE_INTEGER]) assert.throws(() => prepare([item(1)], [product(1, { salePrice })]));
  for (const stock of [-1, 0.5, NaN]) assert.throws(() => prepare([item(1)], [product(1, { stock })]));
  assert.throws(() => prepare([item(1)], [product(1, { name: ' ' })]));
});
test('unsafe arithmetic is rejected', () => {
  assert.throws(() => prepare([item(1, 1000000)], [product(1, { stock: 1000000, salePrice: 1000000000 })]));
});
test('only a configured Colombian mobile in international digits is accepted', () => {
  assert.ok(validWhatsAppNumber(number));
  for (const invalid of [undefined, null, '', ' ', '+573242213841', '57 3242213841', '57324-2213841', '3242213841', '571234567890', number + '0', 'https://wa.me/' + number]) {
    assert.equal(validWhatsAppNumber(invalid), false); assert.throws(() => prepareWhatsAppCheckout([item(1)], [product(1)], invalid));
  }
});
test('accents, emojis, ampersand, hash, newline and percent round-trip through encodeURIComponent', () => {
  const order = prepare([item(1)], [product(1, { name: 'Sérum 💜 & Acné #1 · 20% ¿Sí?' })]);
  assert.equal(new URL(order.url).searchParams.get('text'), order.message); assert.ok(order.url.includes('%F0%9F%92%9C')); assert.match(order.message, /¡Hola, Bloomi Bunny! 💜🌸/);
});
test('does not include acquisition cost, margin, supplier or fabricated delivery cost', () => {
  const order = prepare([item(1)], [product(1, { purchasePrice: 9876, grossProfit: 5124, supplier: 'Proveedor privado' })]);
  assert.doesNotMatch(order.message, /9876|5124|Proveedor privado|purchasePrice|grossProfit/); assert.match(order.message, /Envío: Por confirmar\./);
});
test('shared Gloss Corazón stock never gains inferred tones', () => {
  const order = prepare([item(18)], [product(18, { name: 'Tinta Gloss Corazon Trendy', description: 'Tonos 03, 05 y 06', shade: null })]);
  assert.doesNotMatch(order.message, /Tono:|Tonos|03|05|06/);
});
test('long message keeps every product and falls back without returning a truncated link', () => {
  const products = Array.from({ length: 70 }, (_, index) => product(index + 1, { name: `Artículo ${index + 1} Sérum 💜 ${'especial '.repeat(8)}` }));
  const order = prepare(products.map(p => item(p.id)), products);
  assert.equal(order.url, null); assert.equal(order.lines.length, 70); assert.match(order.message, /70\. Artículo 70/); assert.match(order.message, /TOTAL DE PRODUCTOS: 70/); assert.ok(encodeURIComponent(order.message).length > MAX_WHATSAPP_URL_LENGTH);
});
test('preparing and opening never change cart, catalog or local quantities', () => {
  const cart = Object.freeze([Object.freeze(item(1, 2))]); const catalog = Object.freeze([Object.freeze(product(1))]);
  const before = JSON.stringify({ cart, catalog }); const order = prepare(cart, catalog);
  const popup = { opener: 'previous', location: { replace(url) { assert.equal(url, order.url); } }, close() {} };
  assert.equal(openWhatsAppCheckout(order.url, () => popup), true); assert.equal(popup.opener, null); assert.equal(JSON.stringify({ cart, catalog }), before);
});
test('blocked popup and navigation failure return a recoverable result', () => {
  const order = prepare([item(1)], [product(1)]);
  assert.equal(openWhatsAppCheckout(order.url, () => null), false);
  let closed = false;
  assert.equal(openWhatsAppCheckout(order.url, () => ({ opener: null, location: { replace() { throw new Error('blocked'); } }, close() { closed = true; } })), false); assert.ok(closed);
});
test('invalid destinations and excessive URLs never open a window', () => {
  const unexpected = () => { throw new Error('Should never open'); };
  assert.equal(openWhatsAppCheckout('https://example.com/', unexpected), false); assert.equal(openWhatsAppCheckout(`https://wa.me/${number}?text=${'a'.repeat(8000)}`, unexpected), false);
});
test('existing cart reading, per-SKU limits, removal and availability remain intact', () => {
  assert.deepEqual(readCart('{bad'), []); assert.deepEqual(readCart(JSON.stringify([item(1), item(1), item(2, -1), item(3, 2)])), [item(1), item(3, 2)]);
  const p = product(11, { stock: 2, shade: 'Tono 2' }), q = product(12, { shade: 'Tono 3' });
  const cart = changeQuantity([item(12)], p, 4); assert.equal(cart.find(line => line.productId === 11).quantity, 2);
  assert.deepEqual(changeQuantity(cart, p, 0), [item(12)]); assert.equal(cartLines(cart, [p, q]).length, 2);
});
