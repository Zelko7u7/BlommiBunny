import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { X } from 'lucide-react';
import { useContacts, useNamed } from '../../hooks/useApi';
import images from '../../data/product-images.json';
import type { Product, ProductInput } from '../../types';

export function InventoryDialog({ title, children, onClose, busy = false }: { title: string; children: ReactNode; onClose: () => void; busy?: boolean }) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => { const dialog = ref.current!; dialog.showModal(); return () => dialog.close(); }, []);
  return <dialog ref={ref} className="inventory-dialog" aria-labelledby="inventory-dialog-title" onCancel={e => { e.preventDefault(); if (!busy) onClose(); }} onClick={e => { if (e.target !== e.currentTarget || busy) return; const rect=e.currentTarget.getBoundingClientRect(); if(e.clientX<rect.left||e.clientX>rect.right||e.clientY<rect.top||e.clientY>rect.bottom)onClose(); }}>
    <header><h2 id="inventory-dialog-title">{title}</h2><button type="button" onClick={onClose} disabled={busy} aria-label="Cerrar"><X size={20}/></button></header>
    {children}
  </dialog>;
}

export function ProductCreateDialog({ onClose, onCreate, busy }: { onClose: () => void; onCreate: (data: ProductInput) => Promise<void>; busy: boolean }) {
  return <ProductFormDialog onClose={onClose} onSave={onCreate} busy={busy}/>;
}

export function ProductEditDialog({ product, onClose, onSave, busy }: { product: Product; onClose: () => void; onSave: (data: ProductInput) => Promise<void>; busy: boolean }) {
  return <ProductFormDialog product={product} onClose={onClose} onSave={onSave} busy={busy}/>;
}

function ProductFormDialog({ product, onClose, onSave, busy }: { product?: Product; onClose: () => void; onSave: (data: ProductInput) => Promise<void>; busy: boolean }) {
  const { data: categories = [], isLoading, isError } = useNamed('categories');
  const brands=useNamed('brands');
  const suppliers=useContacts('suppliers');
  const [brandId,setBrandId]=useState(String(product?.brandId??''));
  const [supplierId,setSupplierId]=useState(String(product?.supplierId??''));
  const activeCategories = categories.filter(category => category.active !== false || category.id === product?.categoryId);
  const [code, setCode] = useState(() => product?.code ?? `BB-${crypto.randomUUID().slice(0, 8).toUpperCase()}`);
  const [categoryId, setCategoryId] = useState(String(product?.categoryId ?? ''));
  const [imageUrl, setImageUrl] = useState(product?.imageUrl ?? '');
  const [costKnown, setCostKnown] = useState(product?.costConfirmed ?? true);
  const [error, setError] = useState('');
  async function submit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault(); if (busy) return;
    const form = new FormData(e.currentTarget);
    const categoryId = Number(form.get('categoryId'));
    if (!activeCategories.some(category => category.id === categoryId)) { setError('Selecciona una categoría válida.'); return; }
    setError('');
    try {
      await onSave({
        name: String(form.get('name')).trim(), code: code.trim(), categoryId,
        description: String(form.get('description') ?? '').trim(),
        presentation: String(form.get('presentation') ?? '').trim(),
        shade: String(form.get('shade') ?? '').trim(),
        variantGroup: String(form.get('variantGroup') ?? '').trim() || null,
        brandId: brandId?Number(brandId):null, supplierId: supplierId?Number(supplierId):null, entryDate: String(form.get('entryDate')??'')||null,
        purchasePrice: costKnown ? Number(form.get('purchasePrice')) : product?.purchasePrice ?? 0,
        salePrice: Number(form.get('salePrice')), stock: Number(form.get('stock')),
        minimumStock: Number(form.get('minimumStock')), imageUrl: imageUrl.trim(),
        active: form.has('active'), provisionalData: form.has('provisionalData'), costConfirmed: costKnown,
        ...(product ? { expectedVersion: product.version } : {}),
      });
    } catch (failure: any) { setError(failure?.message ?? 'No fue posible guardar el producto. Inténtalo de nuevo.'); }
  }
  return <InventoryDialog title={product ? 'Editar producto' : 'Nuevo producto'} onClose={onClose} busy={busy}>
    <form onSubmit={submit} className="product-form">
      <fieldset disabled={busy}>
        <div className="product-form-grid">
          <label className="form-wide">Nombre<input name="name" maxLength={140} required autoFocus placeholder="Nombre del producto" defaultValue={product?.name ?? ''}/></label>
          <label>Código<input value={code} onChange={e => setCode(e.target.value)} maxLength={60} required/></label>
          <label>Categoría<select name="categoryId" required disabled={isLoading || isError} value={categoryId} onChange={e => setCategoryId(e.target.value)}><option value="">{isLoading ? 'Cargando…' : 'Selecciona una categoría'}</option>{activeCategories.map(category => <option key={category.id} value={category.id}>{category.name}{category.active === false ? ' (inactiva)' : ''}</option>)}</select></label>
          <label>Marca<select name="brandId" disabled={brands.isLoading||brands.isError} value={brandId} onChange={event=>setBrandId(event.target.value)}><option value="">Sin marca</option>{brands.data?.filter(row=>row.active||row.id===product?.brandId).map(row=><option key={row.id} value={row.id}>{row.name}{!row.active?' (inactiva)':''}</option>)}{product?.brandId&&!brands.data?.some(row=>row.id===product.brandId)&&<option value={product.brandId}>{product.brand||'Marca actual'}</option>}</select></label>
          <label>Proveedor<select name="supplierId" disabled={suppliers.isLoading||suppliers.isError} value={supplierId} onChange={event=>setSupplierId(event.target.value)}><option value="">Sin proveedor</option>{suppliers.data?.filter(row=>row.active||row.id===product?.supplierId).map(row=><option key={row.id} value={row.id}>{row.name}{!row.active?' (inactivo)':''}</option>)}{product?.supplierId&&!suppliers.data?.some(row=>row.id===product.supplierId)&&<option value={product.supplierId}>{product.supplier||'Proveedor actual'}</option>}</select></label>
          <label>Fecha de ingreso<input type="date" name="entryDate" defaultValue={product?.entryDate??''}/></label>
          <label>Costo de compra (COP)<input name="purchasePrice" type="number" min="0" step="0.01" required={costKnown} disabled={!costKnown} defaultValue={product?.costConfirmed ? product.purchasePrice : ''} placeholder={costKnown ? 'Ej. 10000' : 'Por confirmar'}/></label>
          <label>Precio de venta (COP)<input name="salePrice" type="number" min="0" step="0.01" required placeholder="Ej. 15000" defaultValue={product?.salePrice ?? ''}/></label>
          <label>{product ? 'Stock actual' : 'Stock inicial'}<input name="stock" type="number" min="0" max="2147483647" step="1" defaultValue={product?.stock ?? 0} required/></label>
          <label>Stock mínimo<input name="minimumStock" type="number" min="0" max="2147483647" step="1" defaultValue={product?.minimumStock ?? 0} required/></label>
          <label>Presentación<input name="presentation" maxLength={80} placeholder="Ej. 3 ml o kit de 3 unidades" defaultValue={product?.presentation ?? ''}/></label>
          <label>Tono<input name="shade" maxLength={80} defaultValue={product?.shade ?? ''}/></label>
          <label className="form-wide">Grupo de tonalidades<input name="variantGroup" maxLength={80} defaultValue={product?.variantGroup ?? ''} placeholder="Ej. labial-doble-vaquero-toy-story"/><small>Usa el mismo grupo para reunir los tonos en una ficha. Cada tono conserva su código, precio y stock.</small></label>
          <label className="form-wide">Descripción<textarea name="description" maxLength={1200} rows={2} defaultValue={product?.description ?? ''}/></label>
          <label className="form-wide">Foto de la colección<select value={images.some(image => image.imageUrl === imageUrl) ? imageUrl : ''} onChange={e => setImageUrl(e.target.value)}><option value="">Sin foto seleccionada</option>{images.map(image => <option value={image.imageUrl} key={image.imageUrl}>{image.name}</option>)}</select></label>
          <label className="form-wide">Ruta o enlace de otra foto<input value={imageUrl} onChange={e => setImageUrl(e.target.value)} maxLength={500} placeholder="https://… o /products/…"/></label>
        </div>
        <div className="product-form-options">
          <label><input type="checkbox" name="active" defaultChecked={product?.active ?? true}/> Habilitar en la tienda</label>
          <label><input type="checkbox" checked={costKnown} onChange={e => setCostKnown(e.target.checked)}/> Costo de compra confirmado</label>
          <label><input type="checkbox" name="provisionalData" defaultChecked={product?.provisionalData ?? false}/> Precio y stock provisionales</label>
        </div>
        <p className="product-form-hint">La tienda publica productos habilitados con datos confirmados. Los agotados pueden consultarse desde su ficha; los datos provisionales se excluyen de la tienda y de los totales confirmados.</p>
        {(brands.isError||suppliers.isError)&&<p className="product-form-hint" role="status">No pudimos actualizar marcas o proveedores. Sus valores actuales se conservan. <button type="button" onClick={()=>{brands.refetch();suppliers.refetch();}}>Reintentar</button></p>}
        {product && <p className="product-form-hint">Los cambios de stock se registran como ajustes de inventario.</p>}
        {isError && <p className="error" role="alert">No pudimos cargar las categorías.</p>}
        {!isLoading && !isError && activeCategories.length === 0 && <p className="error">Primero <Link to="/admin/categorias" onClick={onClose}>crea una categoría</Link>.</p>}
        {error && <p className="error" role="alert">{error}</p>}
        <footer><button type="button" className="inventory-secondary" onClick={onClose}>Cancelar</button><button className="primary" disabled={isLoading || isError || activeCategories.length === 0}>{busy ? 'Guardando…' : product ? 'Guardar cambios' : 'Crear producto'}</button></footer>
      </fieldset>
    </form>
  </InventoryDialog>;
}

export function ProductDeleteDialog({ product, onClose, onDelete, busy }: { product: Product; onClose: () => void; onDelete: () => Promise<void>; busy: boolean }) {
  const [error, setError] = useState('');
  async function confirm() { if (busy) return; setError(''); try { await onDelete(); } catch (failure: any) { setError(failure?.message ?? 'No fue posible eliminar el producto.'); } }
  return <InventoryDialog title="Eliminar producto" onClose={onClose} busy={busy}>
    <p>¿Eliminar <strong>{product.name}</strong>?</p>
    <p className="product-form-hint">Se quitará de la tienda y del inventario. Sus ventas y movimientos anteriores se conservarán.</p>
    {error && <p className="error" role="alert">{error}</p>}
    <footer><button className="inventory-secondary" onClick={onClose} disabled={busy}>Cancelar</button><button className="inventory-delete" onClick={confirm} disabled={busy}>{busy ? 'Eliminando…' : 'Eliminar producto'}</button></footer>
  </InventoryDialog>;
}
