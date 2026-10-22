import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Minus, Plus, ShoppingBag, ZoomIn, X, Heart } from 'lucide-react';
import { productImage } from '../../services/catalog';
import type { StoreProduct } from '../../types';

export default function ProductDetails({ product, variants = [], onSelectVariant, loading, error, validId, cartQuantity, onAdd, onOpenCart, onRetry, favorite, favoriteDisabled, onFavorite }: {
  variants?: StoreProduct[]; onSelectVariant: (id: number) => void;
  favorite: boolean; favoriteDisabled: boolean; onFavorite: () => void;
  product?: StoreProduct; loading: boolean; error: any; validId: boolean; cartQuantity: number;
  onAdd: (quantity: number) => void; onOpenCart: () => void; onRetry: () => void;
}) {
  const [quantity, setQuantity] = useState(1);
  const [zoom, setZoom] = useState(false);
  useEffect(() => { setQuantity(1); setZoom(false); }, [product?.id]);
  useEffect(() => { document.title = product && !error ? `${product.name} · Bloomi Bunny` : 'Producto · Bloomi Bunny'; return () => { document.title = 'Bloomi Bunny'; }; }, [product?.name, error]);
  if (loading) return <main className="product-detail"><p className="state" role="status">Cargando producto…</p></main>;
  if (!validId || error || !product) {
    const missing = !validId || error?.code === 'NOT_FOUND';
    return <main className="product-detail"><div className="detail-unavailable"><h1>{missing ? 'Producto no disponible' : 'No pudimos cargar el producto'}</h1><p>{missing ? 'Este producto no existe o ya no está publicado en la tienda.' : 'Revisa tu conexión y vuelve a intentarlo.'}</p><Link className="primary" to="/">Volver a la tienda</Link>{!missing && <button className="detail-secondary" onClick={onRetry}>Reintentar</button>}</div></main>;
  }
  const image = productImage(product);
  const available = Math.max(0, product.stock - cartQuantity);
  const selected = Math.min(quantity, Math.max(1, available));
  return <main className="product-detail">
    <nav className="detail-breadcrumb" aria-label="Ubicación"><Link to="/#catalogo"><ArrowLeft size={15}/> Catálogo</Link><span>/</span><span>{product.category}</span></nav>
    <section className="detail-panel">
      <div className="detail-gallery">
        {image ? <><button className="detail-photo" onClick={() => setZoom(true)} aria-label={`Ampliar foto de ${product.name}`}><img src={image} alt={product.name}/><span><ZoomIn size={16}/> Ampliar imagen</span></button>{zoom && <PhotoZoom image={image} name={product.name} onClose={() => setZoom(false)}/>}</> : <div className="detail-photo-placeholder"><span>✦</span><p>Foto próximamente</p></div>}
      </div>
      <div className="detail-purchase">
        <span className="eyebrow">{product.category}</span>
        <h1>{product.name}</h1>
        <button className="detail-secondary detail-favorite" aria-pressed={favorite} disabled={favoriteDisabled} onClick={onFavorite}><Heart size={18} fill={favorite ? 'currentColor' : 'none'}/>{favorite ? 'Quitar de favoritos' : 'Guardar en favoritos'}</button>
        <p className="detail-price">{product.salePrice.toLocaleString('es-CO')} <span>COP</span></p>
        {variants.length > 0 && <fieldset className="detail-tones"><legend>Elige la tonalidad</legend><div>{variants.map(variant => <button type="button" key={variant.id} aria-pressed={variant.id === product.id} onClick={() => onSelectVariant(variant.id)}>{variant.shade || 'Tono sin nombre'}{variant.stock === 0 && <small>Agotado</small>}</button>)}</div><p>Cada tonalidad tiene su propio stock.</p></fieldset>}
        <span className={`detail-stock ${product.stock > 0 ? '' : 'sold-out'}`}>{product.stock > 0 ? 'Disponible' : 'Agotado'}</span>
        <dl className="detail-features">
          {product.brand && <><dt>Marca</dt><dd>{product.brand}</dd></>}
          {product.presentation && <><dt>Presentación</dt><dd>{product.presentation}</dd></>}
          {product.shade && <><dt>Tono</dt><dd>{product.shade}</dd></>}
          <dt>Categoría</dt><dd>{product.category}</dd>
        </dl>
        {product.stock > 0 && <div className="detail-quantity"><span>Cantidad</span><div><button aria-label="Reducir cantidad" disabled={selected <= 1 || available === 0} onClick={() => setQuantity(selected - 1)}><Minus size={16}/></button><output aria-label="Cantidad seleccionada">{selected}</output><button aria-label="Aumentar cantidad" disabled={selected >= available} onClick={() => setQuantity(selected + 1)}><Plus size={16}/></button></div></div>}
        {cartQuantity > 0 && <p className="detail-cart-count">Ya tienes {cartQuantity} {cartQuantity === 1 ? 'unidad' : 'unidades'} en tu carrito.</p>}
        <button className="primary detail-add" disabled={available === 0} onClick={() => onAdd(selected)}><ShoppingBag size={18}/>{product.stock === 0 ? 'Sin stock' : available === 0 ? 'Máximo disponible en el carrito' : 'Agregar al carrito'}</button>
        <button className="detail-secondary" onClick={onOpenCart}>Ver carrito</button>
      </div>
    </section>
    <section className="detail-description"><h2>Descripción del producto</h2><p>{product.description?.trim() || 'La descripción de este producto estará disponible pronto.'}</p></section>
  </main>;
}

function PhotoZoom({ image, name, onClose }: { image: string; name: string; onClose: () => void }) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => { const dialog = ref.current!; dialog.showModal(); return () => dialog.close(); }, []);
  return <dialog ref={ref} className="detail-zoom" aria-label={`Foto ampliada de ${name}`} onCancel={event => { event.preventDefault(); onClose(); }} onClick={event => { if (event.target === event.currentTarget) onClose(); }}><button onClick={onClose} aria-label="Cerrar foto" autoFocus><X/></button><img src={image} alt={name}/></dialog>;
}
