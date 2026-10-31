import type { StoreProduct } from '../types';
import type { CartItem } from './cart';

// Conservative interoperability limit, not a WhatsApp API guarantee.
export const MAX_WHATSAPP_URL_LENGTH = 8000;
export const validWhatsAppNumber = (number: unknown): number is string => typeof number === 'string' && /^573\d{9}$/.test(number);
export const formatCopCents = (cents: number) => `$${new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 }).format(cents / 100)} COP`;

export type OrderLine = { productId: number; name: string; shade?: string; quantity: number; unitPriceCents: number; subtotalCents: number };
export type CartIssue = { productId?: number; message: string };
export type OrderReview = { lines: OrderLine[]; issues: CartIssue[]; units: number; subtotalCents: number };

// Do not reuse cartLines' display clamp here: no requested item may disappear or change silently.
export function reviewWhatsAppCart(cart: CartItem[], products: StoreProduct[]): OrderReview {
  const lines: OrderLine[] = [], issues: CartIssue[] = [];
  const seen = new Set<number>();
  let units = 0, subtotalCents = 0;
  if (!Array.isArray(cart) || cart.length === 0) return { lines, issues: [{ message: 'Agrega productos al carrito para preparar tu pedido.' }], units, subtotalCents };
  const catalog = new Map(products.map(product => [product.id, product]));
  for (const item of cart) {
    const id = item?.productId;
    if (!Number.isSafeInteger(id) || id <= 0 || seen.has(id)) { issues.push({ message: 'Hay una referencia inválida o repetida en el carrito. Revísalo antes de continuar.' }); continue; }
    seen.add(id);
    const product = catalog.get(id);
    const name = product?.name?.trim();
    if (!product || product.active !== true) { issues.push({ productId: id, message: `${name || 'Un producto del carrito'} ya no está disponible. Retíralo para continuar.` }); continue; }
    if (!name || !Number.isSafeInteger(product.stock) || product.stock < 0) { issues.push({ productId: id, message: 'No pudimos validar los datos de un producto. Actualiza el catálogo.' }); continue; }
    if (!Number.isSafeInteger(item.quantity) || item.quantity <= 0) { issues.push({ productId: id, message: `Revisa la cantidad de ${name}.` }); continue; }
    if (product.stock === 0) { issues.push({ productId: id, message: `${name} está agotado. Retíralo para continuar.` }); continue; }
    if (item.quantity > product.stock) { issues.push({ productId: id, message: `La cantidad seleccionada de ${name} supera la disponibilidad actual. Ajústala antes de continuar.` }); continue; }
    const price = product.salePrice, cents = Math.round(price * 100);
    if (typeof price !== 'number' || !Number.isFinite(price) || price < 0 || !Number.isSafeInteger(cents) || Math.abs(price * 100 - cents) > 0.000001) { issues.push({ productId: id, message: `No pudimos validar el precio de ${name}. Actualiza el catálogo.` }); continue; }
    const subtotal = cents * item.quantity;
    if (!Number.isSafeInteger(subtotal) || !Number.isSafeInteger(subtotalCents + subtotal) || !Number.isSafeInteger(units + item.quantity)) { issues.push({ productId: id, message: 'El importe o la cantidad del pedido supera el límite permitido. Revisa el carrito.' }); continue; }
    // Use persisted SKU data only. No parsing of names or inferred shade labels.
    const shade = product.shade?.trim() || undefined;
    lines.push({ productId: id, name, shade, quantity: item.quantity, unitPriceCents: cents, subtotalCents: subtotal });
    units += item.quantity; subtotalCents += subtotal;
  }
  return { lines, issues, units, subtotalCents };
}

export function buildWhatsAppMessage(review: OrderReview): string {
  if (review.issues.length || !review.lines.length) throw new Error('Revisa todos los productos antes de preparar el pedido.');
  const products = review.lines.map((line, index) => [
    `${index + 1}. ${line.name}`,
    ...(line.shade ? [`   Tono: ${line.shade}`] : []),
    `   Cantidad: ${line.quantity}`,
    `   Precio unitario: ${formatCopCents(line.unitPriceCents)}`,
    `   Subtotal: ${formatCopCents(line.subtotalCents)}`,
  ].join('\n')).join('\n\n');
  return `¡Hola, Bloomi Bunny! 💜🌸\n\nQuisiera realizar un pedido de los siguientes productos:\n\n🛍️ MI PEDIDO\n\n${products}\n\n--------------------\n\nTOTAL DE PRODUCTOS: ${review.units}\n\nSUBTOTAL: ${formatCopCents(review.subtotalCents)}\n\nEnvío: Por confirmar.\n\n¿Me podrían confirmar la disponibilidad, el costo de envío y los medios de pago disponibles?\n\n¡Muchas gracias! 💜`;
}

export function prepareWhatsAppCheckout(cart: CartItem[], products: StoreProduct[], number: unknown) {
  if (!validWhatsAppNumber(number)) throw new Error('Los pedidos por WhatsApp no están disponibles: falta configurar un número comercial válido.');
  const review = reviewWhatsAppCart(cart, products), message = buildWhatsAppMessage(review);
  const chatUrl = `https://wa.me/${number}`;
  const url = `${chatUrl}?text=${encodeURIComponent(message)}`;
  return { ...review, message, chatUrl, url: url.length <= MAX_WHATSAPP_URL_LENGTH ? url : null };
}

type ChatWindow = { opener: unknown; location: { replace: (url: string) => void }; close: () => void };
// Called synchronously by the click handler, with no network or clipboard await.
export function openWhatsAppCheckout(url: string, open: () => ChatWindow | null = () => window.open('about:blank', '_blank')): boolean {
  if (!/^https:\/\/wa\.me\/573\d{9}(\?text=|$)/.test(url) || url.length > MAX_WHATSAPP_URL_LENGTH) return false;
  let popup: ChatWindow | null = null;
  try {
    popup = open();
    if (!popup) return false;
    popup.opener = null;
    popup.location.replace(url);
    return true;
  } catch { try { popup?.close(); } catch { /* Copy/manual contact remains available. */ } return false; }
}
