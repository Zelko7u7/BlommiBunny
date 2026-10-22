import images from '../data/product-images.json';
import type { Product, StoreProduct } from '../types';

export function normalizeProductName(name: string): string {
  return name.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]/g, '');
}

const imagesByName = new Map(images.map(image => [normalizeProductName(image.name), image.imageUrl]));

export function productImage(product: Pick<Product, 'name' | 'imageUrl'>): string | undefined {
  return product.imageUrl !== undefined ? product.imageUrl || undefined : imagesByName.get(normalizeProductName(product.name));
}

export type CatalogEntry = {
  key: string;
  name: string;
  imageUrl?: string;
  product?: StoreProduct;
  variants: StoreProduct[];
};

export function buildCatalog(products: StoreProduct[], includeOutOfStock = false, groupVariants = true): CatalogEntry[] {
  const groups = new Map<string, StoreProduct[]>();
  for (const product of products.filter(p => p.active)) {
    const key = groupVariants && product.variantGroup ? `group-${product.variantGroup}` : `product-${product.id}`;
    groups.set(key, [...(groups.get(key) ?? []), product]);
  }
  return [...groups.entries()].flatMap(([key, variants]) => {
    variants.sort((a, b) => (a.shade ?? '').localeCompare(b.shade ?? '', 'es', {numeric: true}));
    const product = variants.find(p => p.stock > 0) ?? variants[0];
    if (!includeOutOfStock && product.stock <= 0) return [];
    return [{key, name: product.name, imageUrl: productImage(product), product, variants}];
  });
}
