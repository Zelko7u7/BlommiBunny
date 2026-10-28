import { Heart, Plus } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { catalogPrice, type CatalogEntry } from '../../services/catalog';
import type { StoreProduct } from '../../types';
import ProductPhoto from './ProductPhoto';

type Props = {
  entry: CatalogEntry;
  backTo: string;
  cartQuantity: number;
  onAdd: (product: StoreProduct) => void;
  individualTone?: boolean;
  favorite?: boolean;
  favoriteDisabled?: boolean;
  onFavorite?: () => void;
};

/** Shared card for the catalog, saved products and related products. */
export default function ProductCard({ entry, backTo, cartQuantity, onAdd, individualTone = false, favorite, favoriteDisabled, onFavorite }: Props) {
  const navigate = useNavigate();
  const product = entry.product;
  if (!product) return null;
  const to = `/producto/${product.id}`;
  const state = { catalogBackTo: backTo };
  const maximum = cartQuantity >= product.stock;
  return <article className="product-card catalog-product-card" data-product-id={product.id} data-product-key={entry.key} onClick={event => {
    if (!(event.target as Element).closest('a,button') && !window.getSelection()?.toString()) navigate(to, { state });
  }}>
    {onFavorite && <button type="button" className={`favorite-toggle ${favorite ? 'is-saved' : ''}`} aria-label={`${favorite ? 'Quitar de' : 'Guardar en'} favoritos: ${entry.name}`} aria-pressed={favorite} disabled={favoriteDisabled} onClick={event => { event.stopPropagation(); onFavorite(); }}><Heart size={19} fill={favorite ? 'currentColor' : 'none'}/></button>}
    <Link className="product-image" to={to} state={state} aria-label={`Ver detalle de ${entry.name}`}><ProductPhoto src={entry.imageUrl} name={entry.name}/></Link>
    <div className="product-info">
      <small>{product.category || 'Belleza'}</small>
      <h3><Link className="product-title-link" to={to} state={state}>{entry.name}</Link></h3>
      {product.presentation && <p>{product.presentation}</p>}
      {product.variantGroup && <p className="catalog-tones">{individualTone ? product.shade : `${entry.variants.length} ${entry.variants.length === 1 ? 'tonalidad' : 'tonalidades'}`}</p>}
      <strong>{`${entry.variants.some(variant => variant.salePrice !== product.salePrice) ? 'Desde ' : ''}${catalogPrice(entry).toLocaleString('es-CO')} COP`}</strong>
      {product.variantGroup ? <Link className="primary catalog-choose-tone" to={to} state={state}>Elegir tonalidad</Link> : <button type="button" className="primary" disabled={product.stock <= 0 || maximum} onClick={event => { event.stopPropagation(); onAdd(product); }}><Plus size={16}/>{product.stock <= 0 ? 'Agotado' : maximum ? 'Máximo disponible' : 'Agregar'}</button>}
    </div>
  </article>;
}
