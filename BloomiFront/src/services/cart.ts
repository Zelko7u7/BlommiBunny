import type { StoreProduct } from '../types';

export type CartItem = { productId: number; quantity: number };

export function readCart(raw: string | null): CartItem[] {
  try {
    const value: unknown = JSON.parse(raw ?? '[]');
    if (!Array.isArray(value)) return [];
    const seen = new Set<number>();
    return value.filter(item => {
      if (!item || !Number.isSafeInteger(item.productId) || item.productId <= 0 || !Number.isSafeInteger(item.quantity) || item.quantity <= 0 || seen.has(item.productId)) return false;
      seen.add(item.productId);
      return true;
    }).map(item => ({ productId: item.productId, quantity: item.quantity }));
  } catch { return []; }
}

export function changeQuantity(items: CartItem[], product: StoreProduct, quantity: number): CartItem[] {
  if (!Number.isSafeInteger(quantity) || quantity < 0) return items;
  const next = items.filter(item => item.productId !== product.id);
  const allowed = product.active ? Math.min(quantity, product.stock) : 0;
  return allowed > 0 ? [...next, { productId: product.id, quantity: allowed }] : next;
}

export function cartLines(items: CartItem[], products: StoreProduct[]) {
  return items.map(item => {
    const product = products.find(product => product.id === item.productId && product.active && product.stock > 0);
    return product ? { product, quantity: Math.min(item.quantity, product.stock) } : null;
  }).filter((line): line is { product: StoreProduct; quantity: number } => line !== null);
}
