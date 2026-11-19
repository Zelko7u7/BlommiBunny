import { buildCatalog, type CatalogEntry } from './catalog';
import type { StoreProduct } from '../types';

// Derived from existing names/presentation/category, not stored or invented tags.
const words = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
const typeRules = {
  balm: /\b(balm|balsamo)\b|\bhidratante de labios\b/,
  tint: /\btintas?\b/,
  gloss: /\bgloss(?:es)?\b/,
  lipstick: /\b(labial|labiales|lipstick)\b/,
  palette: /\b(sombras?|paleta)\b/,
  blush: /\b(rubor|rubores|blush)\b/,
  highlighter: /\b(iluminador|iluminadores)\b|\bpompon cotton\b/,
  serum: /\bserum\b/,
  patches: /\bacne\b.*\b(stickers?|parches?)\b|\b(stickers?|parches?)\b.*\bacne\b/,
  hair: /\b(pelo|cabello|scrunchies?|hebillas?)\b/,
  pouch: /\bcartuchera\b/,
  wallet: /\bmonedero\b/,
  sweater: /\b(sueter(?:es)?|sweaters?)\b/,
  cardigan: /\bcardigan(?:s|es)?\b/,
} as const;
type ProductType = keyof typeof typeRules;
type Family = 'lips' | 'eyes' | 'face' | 'skincare' | 'accessories' | 'tools' | 'knitwear';
const complements: Partial<Record<ProductType, readonly ProductType[]>> = {
  lipstick: ['gloss', 'balm', 'tint'], gloss: ['lipstick', 'balm', 'tint'],
  tint: ['lipstick', 'gloss', 'balm'], balm: ['lipstick', 'gloss', 'tint'],
  palette: ['blush', 'highlighter'], blush: ['highlighter', 'palette'], highlighter: ['blush', 'palette'],
  serum: ['patches'], patches: ['serum'],
  hair: ['pouch', 'wallet'], pouch: ['hair', 'wallet'], wallet: ['hair', 'pouch'],
  sweater: ['cardigan'], cardigan: ['sweater'],
};
const categoryFamilies: Record<string, Family> = {
  labios: 'lips', labiales: 'lips', ojos: 'eyes', sombras: 'eyes', rostro: 'face',
  bases: 'face', correctores: 'face', rubores: 'face', iluminadores: 'face',
  delineadores: 'eyes', pestaninas: 'eyes', brochas: 'tools',
  'cuidado de piel': 'skincare', 'cuidado facial': 'skincare', skincare: 'skincare', accesorios: 'accessories',
  sueter: 'knitwear', sueteres: 'knitwear', sweater: 'knitwear', sweaters: 'knitwear',
  cardigan: 'knitwear', cardigans: 'knitwear', cardiganes: 'knitwear',
};
const nearbyFamilies: Partial<Record<Family, readonly Family[]>> = {
  lips: ['face', 'eyes'], eyes: ['face', 'lips', 'tools'],
  face: ['eyes', 'lips', 'skincare', 'tools'], skincare: ['face'], tools: ['face', 'eyes'],
};
const typeFamilies: Record<ProductType, Family> = {
  balm: 'lips', tint: 'lips', gloss: 'lips', lipstick: 'lips', palette: 'eyes',
  blush: 'face', highlighter: 'face', serum: 'skincare', patches: 'skincare',
  hair: 'accessories', pouch: 'accessories', wallet: 'accessories',
  sweater: 'knitwear', cardigan: 'knitwear',
};
const themeRules = [/\btoy story\b/, /\blotso\b/] as const;

function profile(product: StoreProduct) {
  const name = words(`${product.name} ${product.presentation ?? ''}`);
  const type = (Object.keys(typeRules) as ProductType[]).find(key => typeRules[key].test(name));
  const category = words(product.category);
  const collection = words(`${product.name} ${product.variantGroup ?? ''}`);
  return { type, category, family: categoryFamilies[category] ?? (type ? typeFamilies[type] : undefined), themes: themeRules.map(rule => rule.test(collection)) };
}

/** Up to ten distinct available products, including related category families. */
export function recommendProducts(current: StoreProduct, products: StoreProduct[], limit = 10): CatalogEntry[] {
  const source = profile(current);
  const unique = [...new Map(products.map(product => [product.id, product])).values()].sort((a, b) => a.id - b.id);
  const eligible = unique.filter(product => product.active && product.id !== current.id && (!current.variantGroup || product.variantGroup !== current.variantGroup));
  const score = (product: StoreProduct) => {
    const target = profile(product);
    let value = 0;
    if (source.type && source.type === target.type) value += 50;
    if (source.category && (source.category === target.category || source.family && source.family === target.family)) value += 30;
    if (source.type && target.type && complements[source.type]?.includes(target.type)) value += 25;
    if (source.themes.some((member, index) => member && target.themes[index])) value += 20;
    // Only add explicit neighboring families; never pad with unrelated accessories.
    const accessory = target.type && ['hair', 'pouch', 'wallet'].includes(target.type);
    if (!value && !accessory && source.family && target.family && nearbyFamilies[source.family]?.includes(target.family)) value = 5;
    return value;
  };
  const count = Number.isFinite(limit) ? Math.max(0, Math.min(10, Math.floor(limit))) : 10;
  const compare = (a: { entry: CatalogEntry; score: number }, b: { entry: CatalogEntry; score: number }) => b.score - a.score || a.entry.product!.id - b.entry.product!.id;
  const ranked = buildCatalog(eligible).map(entry => ({ entry, score: Math.max(...entry.variants.filter(product => product.stock > 0).map(score)) }))
    .filter(item => item.score > 0)
    .sort(compare);
  const isRelatedFamily = (item: typeof ranked[number]) => {
    const target = profile(item.entry.product!);
    return source.family && target.family ? source.family !== target.family : source.category !== target.category;
  };
  const otherFamilies = ranked.filter(isRelatedFamily);
  // Reserve up to three places in a full carousel so one large category cannot
  // crowd out all relevant neighboring categories. Keep the strongest matches first.
  const reserved = Math.min(otherFamilies.length, Math.floor(count / 3));
  const selected = [...ranked.filter(item => !isRelatedFamily(item)).slice(0, count - reserved), ...otherFamilies.slice(0, reserved)];
  const keys = new Set(selected.map(item => item.entry.key));
  for (const item of ranked) {
    if (selected.length >= count) break;
    if (!keys.has(item.entry.key)) { selected.push(item); keys.add(item.entry.key); }
  }
  return selected.sort(compare).map(item => item.entry);
}
