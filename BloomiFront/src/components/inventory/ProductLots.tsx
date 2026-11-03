import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { http } from '../../services/http';
import type { Product } from '../../types';
import { InventoryDialog } from './InventoryDialogs';
import { Loading, money } from '../common/Ui';

type Lot={sourceKey:string;reference:string;date:string;remaining:number;unitCost:number|null;costValue:number|null;opening:boolean};
export default function ProductLots({product,onClose}:{product:Product;onClose:()=>void}){
 const query=useQuery({queryKey:['product-lots',product.id],queryFn:()=>http.get<Lot[]>(`/products/${product.id}/lots`).then(r=>r.data),refetchInterval:15000,refetchOnWindowFocus:true});
 const rows=query.data??[];
 return <InventoryDialog title={`Lotes de ${product.name}${product.shade?` · ${product.shade}`:''}`} onClose={onClose}>
  <p>Un solo producto · código {product.code}. Las ventas descuentan primero el saldo inicial y después las compras por fecha de recepción (FIFO).</p>
  {query.isLoading?<Loading/>:query.isError?<p className="error" role="alert">No pudimos cargar los lotes. <button onClick={()=>query.refetch()}>Reintentar</button></p>:<>
   <div className="table-wrap"><table className="product-lots-table"><thead><tr><th>Origen</th><th>Recepción / registro</th><th>Unidades restantes</th><th>Costo/u</th><th>Valor restante</th></tr></thead><tbody>{rows.map(row=><tr key={row.sourceKey}><td data-label="Origen">{row.reference}{row.opening&&<small>Existencias anteriores al seguimiento</small>}</td><td data-label="Recepción / registro">{row.date}</td><td data-label="Unidades restantes">{row.remaining}</td><td data-label="Costo/u">{row.unitCost==null?'Por confirmar':money(row.unitCost)}</td><td data-label="Valor restante">{row.costValue==null&&row.remaining>0?'Por confirmar':money(row.costValue??0)}</td></tr>)}</tbody></table></div>
   <p><strong>{rows.reduce((sum,row)=>sum+row.remaining,0)} unidades</strong> · Costo {rows.some(row=>row.remaining>0&&row.unitCost==null)?'conocido parcial':'total'}: <strong>{money(rows.reduce((sum,row)=>sum+(row.costValue??0),0))}</strong></p>
   <p className="product-form-hint">Los lotes agotados se eliminan. Si solo queda uno, se conserva con cero unidades hasta la siguiente entrada. El historial de compras y ventas permanece.</p>
  </>}
  <footer><button className="inventory-secondary" onClick={onClose}>Cerrar</button><Link className="primary" to={`/admin/compras?new=1&productId=${product.id}`}>Registrar restock</Link></footer>
 </InventoryDialog>;
}
