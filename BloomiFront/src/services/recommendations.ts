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
} as const;
type ProductType = keyof typeof typeRules;
type Family = 'lips' | 'eyes' | 'face' | 'skincare' | 'accessories';
const complements: Partial<Record<ProductType, readonly ProductType[]>> = {
  lipstick: ['gloss', 'balm', 'tint'], gloss: ['lipstick', 'balm', 'tint'],
  tint: ['lipstick', 'gloss', 'balm'], balm: ['lipstick', 'gloss', 'tint'],
  palette: ['blush', 'highlighter'], blush: ['highlighter', 'palette'], highlighter: ['blush', 'palette'],
  serum: ['patches'], patches: ['serum'],
  hair: ['pouch', 'wallet'], pouch: ['hair', 'wallet'], wallet: ['hair', 'pouch'],
};
const categoryFamilies: Record<string, Family> = {
  labios: 'lips', labiales: 'lips', ojos: 'eyes', sombras: 'eyes', rostro: 'face',
  'cuidado de piel': 'skincare', 'cuidado facial': 'skincare', skincare: 'skincare', accesorios: 'accessories',
};
const nearbyFamilies: Partial<Record<Family, readonly Family[]>> = {
  lips: ['face'], eyes: ['face'], face: ['eyes', 'lips'],
};
const themeRules = [/\btoy story\b/, /\blotso\b/] as const;

function profile(product: StoreProduct) {
  const name = words(`${product.name} ${product.presentation ?? ''}`);
  const type = (Object.keys(typeRules) as ProductType[]).find(key => typeRules[key].test(name));
  const category = words(product.category);
  const collection = words(`${product.name} ${product.variantGroup ?? ''}`);
  return { type, category, family: categoryFamilies[category], themes: themeRules.map(rule => rule.test(collection)) };
}

/** At most six distinct, in-stock product families, with stable additive relevance. */
export function recommendProducts(current: StoreProduct, products: StoreProduct[], limit = 6): CatalogEntry[] {
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
    // Only fill with neighboring makeup families. No unrelated padding for skincare/accessories.
    const accessory = target.type && ['hair', 'pouch', 'wallet'].includes(target.type);
    if (!value && !accessory && source.family && target.family && nearbyFamilies[source.family]?.includes(target.family)) value = 5;
    return value;
  };
  const count = Number.isFinite(limit) ? Math.max(0, Math.min(6, Math.floor(limit))) : 6;
  return buildCatalog(eligible).map(entry => ({ entry, score: Math.max(...entry.variants.filter(product => product.stock > 0).map(score)) }))
    .filter(item => item.score > 0)
    .sort((a, b) => b.score - a.score || a.entry.product!.id - b.entry.product!.id)
    .slice(0, count).map(item => item.entry);
}
