import { ShoppingBag, Plus, UserRound, LogOut, Minus, Trash2, Heart, Shield } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import { useStoreProducts, useStoreProduct, useFavorites, validProductId } from '../hooks/useApi';
import ProductDetails from '../components/store/ProductDetails';
import { LegalPage, SocialLinks, StoreFooter } from '../components/store/LegalPages';
import { buildCatalog, productImage, catalogPrice, filterCatalog, type CatalogSort } from '../services/catalog';
import CatalogFilters from '../components/store/CatalogFilters';
import CartDialog from '../components/store/CartDialog';
import ProductPhoto from '../components/store/ProductPhoto';
import { useAuth } from '../contexts/AuthContext';
import { readCart, changeQuantity, cartLines } from '../services/cart';

export default function StorePage() {
  const { session } = useAuth();
  return <Storefront key={session?.username ?? 'guest'} cartKey={`bloomi.cart.${session?.username ? `user:${session.username}` : 'guest'}`}/>;
}

function Storefront({ cartKey }: { cartKey: string }) {
  const { session, logout } = useAuth();
  const { id: productId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const query = searchParams.get('q') ?? '';
  const category = searchParams.get('category') ?? '';
  const requestedSort = searchParams.get('sort');
  const sort: CatalogSort = requestedSort === 'price-asc' || requestedSort === 'price-desc' ? requestedSort : 'name';
  const updateFilter = (key: 'q' | 'category' | 'sort', value: string) => setSearchParams(current => {
    const next = new URLSearchParams(current);
    if (!value || (key === 'sort' && value === 'name')) next.delete(key); else next.set(key, value);
    return next;
  }, { replace: true, preventScrollReset: true });
  const resetFilters = () => setSearchParams(current => {
    const next = new URLSearchParams(current);
    ['q', 'category', 'sort'].forEach(key => next.delete(key));
    return next;
  }, { replace: true, preventScrollReset: true });
  const favoritesPage = location.pathname === '/favoritos';
  const favorites = useFavorites(session?.username);
  const favoriteIds = new Set(favorites.data?.map(product => product.id));
  const requestLogin = () => toast.info('Inicia sesión para guardar tus favoritos', { action: { label: 'Ingresar', onClick: () => navigate('/login') } });
  const toggleFavorite = (id: number) => {
    if (!session) { requestLogin(); return; }
    if (favorites.isLoading || favorites.isError || favorites.change.isPending) return;
    const saved = favoriteIds.has(id);
    favorites.change.mutate({ id, saved }, {
      onSuccess: () => toast.success(saved ? 'Producto quitado de favoritos' : 'Producto guardado en favoritos'),
      onError: (error: any) => toast.error(error?.message || 'No pudimos actualizar tus favoritos. Inténtalo de nuevo.'),
    });
  };
  const favoriteDisabled = !!session && (favorites.isLoading || favorites.isError || favorites.change.isPending);
  const informationalPage = location.pathname === '/nosotros' || location.pathname.startsWith('/politicas/');
  const detail = useStoreProduct(productId);
  const [selectedVariant, setSelectedVariant] = useState<{ routeId?: string; id: number }>();
  const purchaseProduct = selectedVariant?.routeId === productId ? detail.data?.variants?.find(variant => variant.id === selectedVariant?.id) ?? detail.data : detail.data;
  const storeProducts = useStoreProducts();
  const { data = [], isLoading, isError } = storeProducts;
  const [cart, setCart] = useState(() => {
    try { return readCart(localStorage.getItem(cartKey)); } catch { return []; }
  });
  const [cartOpen, setCartOpen] = useState(false);
  const cartTrigger = useRef<HTMLElement | null>(null);
  const openCart = (trigger: HTMLElement) => { cartTrigger.current = trigger; setCartOpen(true); };
  useEffect(() => { try { localStorage.setItem(cartKey, JSON.stringify(cart)); } catch { /* Shopping remains available without local storage. */ } }, [cart, cartKey]);
  const fullCatalog = favoritesPage ? buildCatalog(session ? favorites.data ?? [] : [], true, false) : buildCatalog(data);
  const categories = [...new Set(fullCatalog.flatMap(entry => entry.variants.map(variant => variant.category)))].sort((a, b) => a.localeCompare(b, 'es'));
  const catalog = filterCatalog(fullCatalog, query, category, sort);
  const catalogLoading = favoritesPage ? !!session && favorites.isLoading : isLoading;
  const catalogError = favoritesPage ? !!session && favorites.isError : isError;
  const filtersActive = !!(query.trim() || category);
  const catalogBackTo = `${location.pathname}${location.search}#catalogo`;
  const detailProducts = detail.data && !detail.isError ? [detail.data, ...(detail.data.variants ?? [])] : [];
  const cartProducts = [...detailProducts, ...data.filter(product => !detailProducts.some(current => current.id === product.id) && !(detail.isError && (detail.error as any)?.code === 'NOT_FOUND' && product.id === Number(productId)))];
  const lines = cartLines(cart, cartProducts);
  const count = lines.reduce((sum, line) => sum + line.quantity, 0);
  const total = lines.reduce((sum, line) => sum + line.product.salePrice * line.quantity, 0);
  const currency = (value: number) => `${value.toLocaleString('es-CO')} COP`;
  useEffect(() => {
    if (productId) window.scrollTo(0, 0);
    else if (location.hash === '#catalogo') document.getElementById('catalogo')?.scrollIntoView();
    else if (location.hash === '#nosotros') document.getElementById('nosotros')?.scrollIntoView();
    else window.scrollTo(0, 0);
  }, [productId, location.hash, location.pathname]);

  return <div className="store">
    <header className="store-header">
      <Link to="/" className="store-brand" aria-label="Bloomi Bunny, inicio"><div className="brand-mark">B</div><div><strong>Bloomi Bunny</strong><span>Belleza que florece</span></div></Link>
      <nav aria-label="Navegación principal"><Link to="/#catalogo">Catálogo</Link><Link to="/nosotros">Nosotros</Link></nav>
      <div className="store-actions">
        {session ? <><span className="store-username">Hola, {session.username}</span>{session.role === 'ADMIN' && <Link to="/admin" aria-label="Administración"><Shield/><span className="admin-link-label">Administración</span></Link>}<button onClick={logout} aria-label="Cerrar sesión"><LogOut/></button></> : <Link to="/login" aria-label="Iniciar sesión"><UserRound/></Link>}
        <button className="favorites-header" onClick={() => session ? navigate('/favoritos') : requestLogin()} aria-label={`Favoritos${session ? `: ${favorites.data?.length ?? 0} productos` : ', inicia sesión'}`} title="Favoritos"><Heart/>{session && <b>{favorites.data?.length ?? 0}</b>}</button>
        <button onClick={event => openCart(event.currentTarget)} aria-label={`Carrito con ${count} productos`}><ShoppingBag/><b>{count}</b></button>
      </div>
    </header>
    {informationalPage ? <LegalPage/> : productId ? <ProductDetails key={productId} backTo={location.state?.catalogBackTo ?? '/#catalogo'} product={purchaseProduct} variants={detail.data?.variants} onSelectVariant={id => setSelectedVariant({routeId: productId, id})} loading={detail.isLoading} error={detail.error} validId={validProductId(productId)} favorite={favoriteIds.has(purchaseProduct?.id ?? Number(productId))} favoriteDisabled={favoriteDisabled} onFavorite={() => toggleFavorite(purchaseProduct?.id ?? Number(productId))} cartQuantity={purchaseProduct ? Math.min(cart.find(item => item.productId === purchaseProduct.id)?.quantity ?? 0, purchaseProduct.stock) : 0} onRetry={() => detail.refetch()} onOpenCart={openCart} onAdd={quantity => {
      if (!purchaseProduct || detail.isError) return;
      const product = purchaseProduct;
      setCart(current => changeQuantity(current, product, (current.find(item => item.productId === product.id)?.quantity ?? 0) + quantity));
      toast.success('Producto agregado al carrito', {description: product.shade ? `${product.name} · ${product.shade}` : product.name});
    }}/> : <>
    {!favoritesPage && <section className="store-hero">
      <div><span className="eyebrow">MAQUILLAJE · CUIDADO · BELLEZA</span><h1>Tu ritual de belleza,<br/><i>hecho para florecer.</i></h1><p>Descubre productos seleccionados para resaltar tu belleza todos los días.</p><a className="primary" href="#catalogo">Explorar colección</a></div>
      <div className="hero-flower">✦</div>
    </section>}
    <main id="catalogo" className="store-catalog">
      <div className="store-section-title"><div><span className="eyebrow">{favoritesPage ? 'TU COLECCIÓN' : 'NUESTRA COLECCIÓN'}</span><h2>{favoritesPage ? 'Mis favoritos' : 'Encuentra tus favoritos'}</h2></div><span role="status" aria-live="polite" aria-atomic="true">{catalogLoading ? 'Consultando…' : catalogError ? 'Sin actualizar' : `${catalog.length}${filtersActive ? ` de ${fullCatalog.length}` : ''} ${catalog.length === 1 && !filtersActive ? 'producto' : 'productos'}`}</span></div>
      {(!favoritesPage || session) && <CatalogFilters query={query} category={category} sort={sort} categories={categories} onChange={updateFilter} onReset={resetFilters}/>}
      {favoritesPage && !session ? <p className="state">Inicia sesión para guardar y consultar tus favoritos. <Link to="/login">Ingresar</Link></p> : <>
        {catalogLoading && <p className="catalog-notice" role="status">{favoritesPage ? 'Cargando favoritos…' : 'Consultando precios y disponibilidad…'}</p>}
        {catalogError && <p className="catalog-notice catalog-error" role="alert">No pudimos actualizar {favoritesPage ? 'tus favoritos' : 'los productos'}. Revisa tu conexión y vuelve a intentarlo. <button type="button" onClick={() => favoritesPage ? favorites.refetch() : storeProducts.refetch()}>Reintentar</button></p>}
        {!catalogLoading && !catalogError && catalog.length === 0 && (filtersActive ? <div className="catalog-empty"><SearchEmpty/><h3>No encontramos coincidencias</h3><p>Prueba con otro nombre o categoría.</p><button type="button" className="primary" onClick={resetFilters}>Ver {favoritesPage ? 'todos mis favoritos' : 'todos los productos'}</button></div> : <p className="state">{favoritesPage ? 'Aún no tienes favoritos. Pulsa el corazón de los productos que te gusten.' : 'Pronto encontrarás nuevos productos aquí.'}</p>)}
      </>}
      {session && favorites.isError && !favoritesPage && <p role="alert" className="catalog-notice">No pudimos consultar tus favoritos. <button onClick={() => favorites.refetch()}>Reintentar</button></p>}
      <div className="product-grid">
        {catalog.map(entry => <article className="product-card" key={entry.key}>
          <button className={`favorite-toggle ${favoriteIds.has(entry.product!.id) ? 'is-saved' : ''}`} aria-label={`${favoriteIds.has(entry.product!.id) ? 'Quitar de' : 'Guardar en'} favoritos: ${entry.name}`} aria-pressed={favoriteIds.has(entry.product!.id)} disabled={favoriteDisabled} onClick={() => toggleFavorite(entry.product!.id)}><Heart size={19} fill={favoriteIds.has(entry.product!.id) ? 'currentColor' : 'none'}/></button>
          <Link className="product-image" to={`/producto/${entry.product!.id}`} state={{catalogBackTo}} aria-label={`Ver detalle de ${entry.name}`}><ProductPhoto src={entry.imageUrl} name={entry.name}/></Link>
          <div className="product-info">
            <small>{entry.product?.category || 'Belleza'}</small>
            <h3><Link className="product-title-link" to={`/producto/${entry.product!.id}`} state={{catalogBackTo}}>{entry.name}</Link></h3>
            {entry.product?.presentation && <p>{entry.product.presentation}</p>}
            {entry.product?.variantGroup && <p className="catalog-tones">{favoritesPage ? entry.product.shade : `${entry.variants.length} ${entry.variants.length === 1 ? 'tonalidad' : 'tonalidades'}`}</p>}
            <strong>{`${entry.variants.some(v => v.salePrice !== entry.product!.salePrice) ? 'Desde ' : ''}${catalogPrice(entry).toLocaleString('es-CO')} COP`}</strong>
            {entry.product?.variantGroup ? <Link className="primary catalog-choose-tone" to={`/producto/${entry.product.id}`} state={{catalogBackTo}}>Elegir tonalidad</Link> : entry.product ? <button className="primary" disabled={entry.product.stock <= 0 || (cart.find(item => item.productId === entry.product!.id)?.quantity ?? 0) >= entry.product.stock} onClick={() => { setCart(current => changeQuantity(current, entry.product!, (current.find(item => item.productId === entry.product!.id)?.quantity ?? 0) + 1)); toast.success("Producto agregado al carrito", { description: entry.name }); }}><Plus size={16}/> {entry.product.stock <= 0 ? 'Agotado' : (cart.find(item => item.productId === entry.product!.id)?.quantity ?? 0) >= entry.product.stock ? 'Máximo disponible' : 'Agregar'}</button> : null}
          </div>
        </article>)}
      </div>
    </main>
    {!favoritesPage && <section id="nosotros" className="store-about"><h2>Belleza que florece, desde Pasto</h2><p>Somos una tienda virtual de maquillaje y cuidado en Pasto, Nariño, Colombia. Conoce nuestros canales oficiales y consulta la información de la tienda.</p><SocialLinks/><Link className="about-more" to="/nosotros">Conocer Bloomi Bunny →</Link></section>}
    </>}
    <StoreFooter/>
    {cartOpen && <CartDialog onClose={() => setCartOpen(false)} returnFocusTo={cartTrigger.current}>
        {isLoading ? <p role="status">Consultando tu carrito…</p> : isError ? <p role="alert">No pudimos confirmar los precios y existencias. <button type="button" onClick={() => storeProducts.refetch()}>Reintentar</button></p> : <>
          {lines.length === 0 && <p className="cart-empty">Tu carrito está vacío. Explora la colección y agrega tus favoritos.</p>}
          {cart.length > lines.length && <p className="catalog-notice">Algunos productos ya no están disponibles y no se incluyen en el total.</p>}
          <div className="cart-items">{lines.map(({ product, quantity }) => <article className="cart-item" key={product.id}>
            <div className="cart-photo"><ProductPhoto src={productImage(product)} name={product.name} compact/></div>
            <div><h3><Link to={`/producto/${product.id}`} onClick={() => { setSelectedVariant({routeId: String(product.id), id: product.id}); setCartOpen(false); }}>{product.name}</Link></h3>{product.shade && <p className="cart-tone">{product.shade}</p>}<p>{currency(product.salePrice)}</p><div className="cart-quantity">
              <button aria-label={`Reducir cantidad de ${product.name}`} onClick={() => setCart(current => changeQuantity(current, product, quantity - 1))}><Minus size={14}/></button>
              <span aria-label={`Cantidad de ${product.name}${product.shade ? `, ${product.shade}` : ''}`} aria-live="polite">{quantity}</span>
              <button aria-label={`Aumentar cantidad de ${product.name}`} disabled={quantity >= product.stock} onClick={() => setCart(current => changeQuantity(current, product, quantity + 1))}><Plus size={14}/></button>
              <button aria-label={`Eliminar ${product.name}`} onClick={() => setCart(current => current.filter(item => item.productId !== product.id))}><Trash2 size={16}/></button>
            </div></div>
            <strong>{currency(product.salePrice * quantity)}</strong>
          </article>)}</div>
          <div className="cart-total"><span>Total de productos</span><strong>{currency(total)}</strong></div>
          {lines.length > 0 && <p className="catalog-notice">La compra y el pago estarán disponibles próximamente.</p>}
        </>}
        {cart.length > 0 && <button className="cart-clear" onClick={() => { setCart([]); toast.info('Carrito vaciado'); }}>Vaciar carrito</button>}
        <button className="primary" onClick={() => setCartOpen(false)}>Seguir explorando</button>
    </CartDialog>}
  </div>;
}

function SearchEmpty() { return <span className="catalog-empty-icon" aria-hidden="true">✦</span>; }
