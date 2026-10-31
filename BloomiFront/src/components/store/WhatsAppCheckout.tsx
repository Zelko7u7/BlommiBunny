import { useEffect, useId, useRef, useState } from 'react';
import { Copy, ExternalLink } from 'lucide-react';
import type { StoreProduct } from '../../types';
import type { CartItem } from '../../services/cart';
import { openWhatsAppCheckout, prepareWhatsAppCheckout, reviewWhatsAppCart, validWhatsAppNumber } from '../../services/whatsappCheckout';

function WhatsAppIcon() {
  return <svg viewBox="0 0 24 24" width="21" height="21" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true"><path d="M20.5 11.6a8.5 8.5 0 0 1-12.7 7.4L3 20.5l1.5-4.6a8.5 8.5 0 1 1 16-4.3Z"/><path d="m8 7.3 1.3-.2 1.1 2.4-1 1.1a7.4 7.4 0 0 0 3.9 3.9l1.1-1 2.3 1.1-.1 1.4c-.2.8-1 1.2-1.8 1-4.1-.8-7.4-4.1-8.1-8.1-.2-.8.3-1.5 1.3-1.6Z"/></svg>;
}

export default function WhatsAppCheckout({ cart, products, checking, failed, onRemove, onRefresh }: {
  cart: CartItem[]; products: StoreProduct[]; checking: boolean; failed: boolean;
  onRemove: (id: number) => void; onRefresh: () => void;
}) {
  const number = import.meta.env.VITE_WHATSAPP_NUMBER;
  const review = reviewWhatsAppCart(cart, products);
  const configured = validWhatsAppNumber(number);
  const ready = configured && !checking && !failed && review.issues.length === 0;
  const order = ready ? prepareWhatsAppCheckout(cart, products, number) : null;
  const [status, setStatus] = useState('');
  const [fallback, setFallback] = useState(false);
  const [copying, setCopying] = useState(false);
  const textRef = useRef<HTMLTextAreaElement>(null);
  const copyAttempt = useRef(0);
  const descriptionId = useId();
  const signature = JSON.stringify({ cart, products: products.map(p => [p.id, p.name, p.shade, p.salePrice, p.stock, p.active]), number });
  useEffect(() => { copyAttempt.current++; setStatus(''); setFallback(false); setCopying(false); }, [signature]);
  function open() {
    if (!order?.url) return;
    const opened = openWhatsAppCheckout(order.url);
    setFallback(!opened);
    setStatus(opened ? 'Se solicitó abrir WhatsApp. Revisa el mensaje y envíalo manualmente. Tu carrito se conserva.' : 'No pudimos abrir WhatsApp. Copia el pedido y abre la conversación manualmente.');
  }
  async function copy() {
    if (!order || copying) return;
    const attempt = ++copyAttempt.current;
    setCopying(true);
    try {
      await navigator.clipboard.writeText(order.message);
      if (attempt === copyAttempt.current) setStatus('Pedido copiado. Abre WhatsApp, pega el mensaje y envíalo cuando lo hayas revisado.');
    } catch {
      if (attempt === copyAttempt.current) { setFallback(true); setStatus('No pudimos copiar automáticamente. Selecciona el mensaje completo y cópialo manualmente.'); }
    } finally { if (attempt === copyAttempt.current) setCopying(false); }
  }
  return <section className="whatsapp-checkout" aria-label="Solicitar pedido por WhatsApp">
    {checking && <p className="checkout-notice" role="status">Actualizando precios y disponibilidad…</p>}
    {failed && <p className="checkout-notice" role="alert">No pudimos actualizar el catálogo. Reintenta antes de preparar el pedido.</p>}
    {!configured && <p className="checkout-notice" role="alert">Los pedidos por WhatsApp no están disponibles: falta configurar un número comercial válido.</p>}
    {!checking && !failed && cart.length > 0 && review.issues.length > 0 && <div className="checkout-issues" role="alert"><p>Revisa el carrito para incluir todos los artículos correctamente:</p><ul>{review.issues.map((issue, index) => <li key={index}>{issue.message}{issue.productId !== undefined && <button type="button" className="checkout-text-button" onClick={() => onRemove(issue.productId!)}>Retirar este artículo</button>}</li>)}</ul></div>}
    <button type="button" className="primary whatsapp-order-button" disabled={!order?.url} onClick={open} aria-describedby={descriptionId}><WhatsAppIcon/>Realizar pedido por WhatsApp</button>
    <p id={descriptionId} className="checkout-explanation">Tu pedido se enviará por WhatsApp para confirmar disponibilidad, envío y medios de pago. No se realizará ningún cobro en esta página.</p>
    <p className="checkout-explanation">Tú revisas el mensaje y decides enviarlo. La disponibilidad definitiva se confirma en la conversación; no se reservan productos.</p>
    {order && <>
      {!order.url && <p className="checkout-notice" role="status">Tu pedido es demasiado largo para abrirlo completo en un enlace. Copia el mensaje completo y pégalo en WhatsApp; no se ha eliminado ningún artículo.</p>}
      <div className="checkout-alternatives"><button type="button" className="checkout-secondary" disabled={copying} onClick={copy}><Copy size={17}/>{copying ? 'Copiando…' : 'Copiar pedido completo'}</button><a className="checkout-secondary" href={order.chatUrl} target="_blank" rel="noopener noreferrer"><ExternalLink size={17}/>Abrir WhatsApp sin mensaje</a></div>
      <p className="checkout-explanation">Si no se abre la app, puedes usar WhatsApp Web desde el enlace. Si falla, copia el pedido y contacta al número {number}.</p>
      <details className="checkout-message" open={fallback || !order.url}><summary>Ver mensaje y copiar manualmente</summary><label>Pedido completo<textarea ref={textRef} readOnly value={order.message} rows={12} onFocus={event => event.currentTarget.select()}/></label><button type="button" className="checkout-secondary" onClick={() => { textRef.current?.focus(); textRef.current?.select(); }}>Seleccionar todo el mensaje</button></details>
    </>}
    {status && <p className="checkout-notice" role="status" aria-live="polite">{status}</p>}
    {(failed || review.issues.length > 0 && cart.length > 0) && <button type="button" className="checkout-text-button" disabled={checking} onClick={onRefresh}>Actualizar catálogo</button>}
  </section>;
}
