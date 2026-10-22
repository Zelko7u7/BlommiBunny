import { productImage } from '../services/catalog';
import { MoreHorizontal, Plus, Eye, EyeOff, Trash2, Pencil } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { createPortal } from 'react-dom';
import { toast } from 'sonner';
import { useProducts, useProductActions, useInventorySummary } from '../hooks/useApi';
import { ProductCreateDialog, ProductEditDialog, ProductDeleteDialog } from '../components/inventory/InventoryDialogs';
import type { Product, ProductInput } from '../types';
import { Card, Empty, Loading, money, PageHeader, SearchBox } from '../components/common/Ui';

export default function InventoryPage() {
  const [params,setParams] = useSearchParams();
  const q=params.get('q')??'';
  const stock=params.get('stock')==='low'?'low':params.get('stock')==='out'?'out':'all';
  const setQ=(value:string)=>setParams(current=>{const next=new URLSearchParams(current);if(value)next.set('q',value);else next.delete('q');return next;},{replace:true});
  const { data = [], isLoading, isError } = useProducts(q,stock);
  const summary = useInventorySummary(q,stock);
  const totals = summary.data;
  const { create, update, setActive, remove } = useProductActions();
  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<Product | null>(null);
  const [editing, setEditing] = useState<Product | null>(null);
  const [menu, setMenu] = useState<{ id: number; left: number; top: number } | null>(null);
  const busy = create.isPending || update.isPending || setActive.isPending || remove.isPending;
  const menuProduct = data.find(p => p.id === menu?.id);
  async function createProduct(input: ProductInput) {
    await create.mutateAsync(input); setCreating(false); toast.success('Producto creado');
  }
  async function toggleProduct(product: Product) {
    setMenu(null);
    try { await setActive.mutateAsync({ id: product.id, active: !product.active }); toast.success(product.active ? 'Producto deshabilitado en la tienda' : 'Producto habilitado'); }
    catch (failure: any) { toast.error(failure?.message ?? 'No fue posible cambiar el estado.'); }
  }
  return <>
    <PageHeader eyebrow="CATÁLOGO" title="Inventario" subtitle="Controla existencias, precios y disponibilidad de tus productos." action={<button className="primary" onClick={() => { setMenu(null); setCreating(true); }} disabled={busy}><Plus/> Nuevo producto</button>}/>
    {summary.isLoading && <p role="status">Calculando el valor actual del inventario…</p>}
    {summary.isError ? <p className="error" role="alert">No pudimos actualizar los totales del inventario. <button onClick={() => summary.refetch()}>Reintentar</button></p> : totals && <>
      <div className="inventory-summary">
        <Card><span>Unidades confirmadas {q || stock!=='all' ? 'en los resultados' : 'en inventario'}</span><strong>{totals.confirmedUnits}</strong></Card>
        <Card><span>Valor del stock a precio de venta</span><strong>{money(totals.saleValue)}</strong><small>Stock actual × precio de venta actual</small></Card>
        <Card><span>{totals.unknownCostUnits > 0 ? 'Costo de compra registrado (parcial)' : 'Valor del stock a costo de compra'}</span><strong>{money(totals.confirmedCostValue)}</strong></Card>
      </div>
      {totals.unknownCostUnits > 0 && <p className="catalog-notice" role="status">Falta registrar el costo de compra de {totals.unknownCostProducts} productos ({totals.unknownCostUnits} unidades). Su valor de venta está incluido; el costo total y las ganancias aún están incompletos.</p>}
      {totals.provisionalProducts > 0 && <p className="catalog-notice" role="status">{totals.provisionalProducts} productos tienen precio y stock provisionales ({totals.provisionalUnits} unidades). Se excluyen de los totales confirmados.</p>}
    </>}
    <Card>
      <div className="table-tools inventory-filters"><SearchBox value={q} onChange={setQ} placeholder="Buscar por nombre o código…"/><label>Existencias<select aria-label="Filtrar existencias" value={stock} onChange={event=>setParams(current=>{const next=new URLSearchParams(current);if(event.target.value==='all')next.delete('stock');else next.set('stock',event.target.value);return next;},{replace:true})}><option value="all">Todas</option><option value="low">Stock bajo</option><option value="out">Agotados</option></select></label>{(q||stock!=='all')&&<button type="button" className="inventory-secondary" onClick={()=>setParams({},{replace:true})}>Limpiar filtros</button>}<span>{!summary.isError && totals ? `${data.length} de ${totals.totalProducts}` : data.length} productos</span></div>
      {isLoading ? <Loading/> : isError ? <p className="error" role="alert">No pudimos cargar el inventario. Revisa tu sesión y vuelve a intentarlo.</p> : data.length === 0 ? <Empty text="No encontramos productos."/> : <div className="table-wrap"><table>
        <thead><tr><th>Producto</th><th>Categoría</th><th>Costo/u</th><th>Venta/u</th><th>Ganancia/u</th><th>Stock</th><th>Valor stock (venta)</th><th>Estado</th><th/></tr></thead>
        <tbody>{data.map(p => <tr key={p.id}>
          <td><div className="product-cell"><div className="product-thumb">{productImage(p) ? <img src={productImage(p)} alt={p.name} loading="lazy"/> : p.name[0]}</div><div><strong>{p.name}</strong>{p.shade && <span>Tono: {p.shade}</span>}{p.provisionalData && <span className="provisional-label">Precio y stock provisionales</span>}<span>{p.code} · {p.presentation || 'Sin presentación'}</span></div></div></td>
          <td>{p.category}</td><td>{p.costConfirmed === false ? "Por confirmar" : money(p.purchasePrice)}</td><td><strong>{money(p.salePrice)}</strong></td><td className="profit">{p.profit == null || p.provisionalData ? "Por confirmar" : money(p.profit)}</td><td>{p.stock} <small>uds.</small></td><td>{p.provisionalData ? "Por confirmar" : money(p.stock * p.salePrice)}</td>
          <td><span className={`badge ${!p.active ? 'disabled' : p.stockStatus.toLowerCase()}`}>{!p.active ? 'Deshabilitado' : p.stockStatus === 'OK' ? 'Disponible' : p.stockStatus === 'LOW' ? 'Stock bajo' : 'Agotado'}</span></td>
          <td><button className="icon-button product-actions-trigger" data-inventory-menu aria-label={`Acciones de ${p.name}`} aria-haspopup="menu" aria-expanded={menu?.id === p.id} disabled={busy} onClick={event => {
            if (menu?.id === p.id) { setMenu(null); return; }
            const rect = event.currentTarget.getBoundingClientRect();
            setMenu({ id: p.id, left: Math.max(8, Math.min(rect.right - 220, window.innerWidth - 228)), top: rect.bottom + 200 < window.innerHeight ? rect.bottom + 6 : Math.max(8, rect.top - 190) });
          }}><MoreHorizontal/></button></td>
        </tr>)}</tbody>
      </table></div>}
    </Card>
    {menu && menuProduct && <ProductActionMenu product={menuProduct} left={menu.left} top={menu.top} onClose={() => setMenu(null)} onEdit={() => { setMenu(null); setEditing(menuProduct); }} onToggle={() => toggleProduct(menuProduct)} onCreate={() => { setMenu(null); setCreating(true); }} onDelete={() => { setMenu(null); setDeleting(menuProduct); }}/>}
    {creating && <ProductCreateDialog busy={create.isPending} onClose={() => setCreating(false)} onCreate={createProduct}/>}
    {editing && <ProductEditDialog key={editing.id} product={editing} busy={update.isPending} onClose={() => setEditing(null)} onSave={async input => { await update.mutateAsync({ id: editing.id, data: input }); setEditing(null); toast.success('Producto actualizado'); }}/>}
    {deleting && <ProductDeleteDialog product={deleting} busy={remove.isPending} onClose={() => setDeleting(null)} onDelete={async () => { await remove.mutateAsync(deleting.id); setDeleting(null); toast.success('Producto eliminado del inventario'); }}/>}
  </>;
}

function ProductActionMenu({ product, left, top, onClose, onEdit, onToggle, onCreate, onDelete }: { product: Product; left: number; top: number; onClose: () => void; onEdit: () => void; onToggle: () => void; onCreate: () => void; onDelete: () => void }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    ref.current?.querySelector<HTMLButtonElement>('button')?.focus();
    const outside = (event: PointerEvent) => { if (!ref.current?.contains(event.target as Node) && !(event.target as Element).closest?.('[data-inventory-menu]')) onClose(); };
    const close = () => onClose();
    document.addEventListener('pointerdown', outside); window.addEventListener('scroll', close, true); window.addEventListener('resize', close);
    return () => { document.removeEventListener('pointerdown', outside); window.removeEventListener('scroll', close, true); window.removeEventListener('resize', close); };
  }, [onClose]);
  return createPortal(<div ref={ref} role="menu" aria-label={`Acciones de ${product.name}`} className="product-menu" style={{ left, top }} onKeyDown={event => {
    if (event.key === 'Escape') { onClose(); return; }
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault(); const buttons = Array.from(ref.current!.querySelectorAll('button'));
      const index = buttons.indexOf(document.activeElement as HTMLButtonElement);
      buttons[(index + (event.key === 'ArrowDown' ? 1 : buttons.length - 1)) % buttons.length].focus();
    }
  }}>
    <button role="menuitem" onClick={onEdit}><Pencil size={17}/> Editar producto</button>
    <button role="menuitem" onClick={onToggle}>{product.active ? <EyeOff size={17}/> : <Eye size={17}/>} {product.active ? 'Deshabilitar producto' : 'Habilitar producto'}</button>
    <button role="menuitem" onClick={onCreate}><Plus size={17}/> Nuevo producto</button>
    <button role="menuitem" className="danger-text" onClick={onDelete}><Trash2 size={17}/> Eliminar producto</button>
  </div>, document.body);
}
