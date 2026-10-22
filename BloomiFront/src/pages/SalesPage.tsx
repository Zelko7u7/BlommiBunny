import { useRef, useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, ShoppingBag, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { useSearchParams } from 'react-router-dom';
import { api } from '../services/api';
import { normalizeProductName } from '../services/catalog';
import { InventoryDialog } from '../components/inventory/InventoryDialogs';
import { Card, Loading, PageHeader } from '../components/common/Ui';
import type { CreateSale, PaymentMethod, Product, Sale, SaleChannel, SaleFilters } from '../types';
import '../styles/sales.css';

const channels:Record<SaleChannel,string> = { WHATSAPP:'WhatsApp', INSTAGRAM:'Instagram', OTHER:'Otro canal' };
const money = (value:number) => new Intl.NumberFormat('es-CO',{style:'currency',currency:'COP',minimumFractionDigits:0,maximumFractionDigits:2}).format(value);
const payments:Record<PaymentMethod,string> = { CASH:'Efectivo', TRANSFER:'Transferencia', CARD:'Tarjeta', OTHER:'Otro' };
const date = (value:string) => new Date(value).toLocaleString('es-CO', { timeZone:'America/Bogota', dateStyle:'medium', timeStyle:'short' });
const initialFilters:SaleFilters = { q:'',from:'',to:'',channel:'',status:'all',page:0 };
function errorMessage(error:unknown) { return (error as {message?:string})?.message || 'No fue posible completar la operación. Inténtalo de nuevo.'; }

export default function SalesPage() {
 const [params,setParams] = useSearchParams();
 const [filters,setFilters] = useState<SaleFilters>(()=>({q:params.get('q')??'',from:/^\d{4}-\d{2}-\d{2}$/.test(params.get('from')??'')?params.get('from')!:'',to:/^\d{4}-\d{2}-\d{2}$/.test(params.get('to')??'')?params.get('to')!:'',channel:['WHATSAPP','INSTAGRAM','OTHER'].includes(params.get('channel')??'')?params.get('channel')!:'',status:['confirmed','cancelled'].includes(params.get('status')??'')?params.get('status')!:'all',page:0}));
 const [creating,setCreating] = useState(params.get('new')==='1');
 const closeCreate=()=>{setCreating(false);setParams(current=>{const next=new URLSearchParams(current);next.delete('new');return next;},{replace:true});};
 const [selected,setSelected] = useState<number|null>(null);
 const [cancelling,setCancelling] = useState<Sale|null>(null);
 const qc = useQueryClient();
 const invalidDates = !!(filters.from && filters.to && filters.from>filters.to);
 const history = useQuery({queryKey:['sales',filters],queryFn:()=>api.sales(filters),enabled:!invalidDates,refetchOnWindowFocus:true,refetchInterval:15000});
 const detail = useQuery({queryKey:['sale',selected],queryFn:()=>api.sale(selected!),enabled:selected!==null,refetchOnWindowFocus:true});
 const refresh = async () => { await Promise.all(['sales','sale','sale-products','products','inventory-summary','store-products','store-product','favorites','dashboard'].map(key=>qc.invalidateQueries({queryKey:[key]}))); };
 const create = useMutation({mutationFn:api.createSale,onSuccess:refresh});
 const cancel = useMutation({mutationFn:({id,reason}:{id:number;reason:string})=>api.cancelSale(id,reason),onSuccess:refresh});
 const changeFilter = (key:keyof SaleFilters,value:string) => setFilters(current=>({...current,[key]:value,page:0}));
 return <div className="sales-page">
  <PageHeader eyebrow="OPERACIÓN" title="Ventas" subtitle="Registra las ventas confirmadas de WhatsApp, Instagram y otros canales." action={<button className="primary" disabled={!history.data} onClick={()=>setCreating(true)}><Plus/> Registrar venta</button>}/>
  <p className="sales-intro">Cada venta registrada descuenta sus productos del inventario. Aquí dejas constancia del pago recibido; no se realiza un cobro en línea.</p>
  <Card>
   <form className="sales-filters" onSubmit={event=>event.preventDefault()} aria-label="Filtrar ventas">
    <label className="sales-search">Buscar venta<input type="search" value={filters.q} maxLength={120} onChange={event=>changeFilter('q',event.target.value)} placeholder="Número, cliente, producto o tono"/></label>
    <label>Desde<input type="date" value={filters.from} onChange={event=>changeFilter('from',event.target.value)}/></label>
    <label>Hasta<input type="date" value={filters.to} min={filters.from||undefined} onChange={event=>changeFilter('to',event.target.value)}/></label>
    <label>Canal<select value={filters.channel} onChange={event=>changeFilter('channel',event.target.value)}><option value="">Todos</option>{Object.entries(channels).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>
    <label>Estado<select value={filters.status} onChange={event=>changeFilter('status',event.target.value)}><option value="all">Todas</option><option value="confirmed">Confirmadas</option><option value="cancelled">Anuladas</option></select></label>
    <button type="button" className="inventory-secondary" onClick={()=>setFilters(initialFilters)}>Restablecer</button>
   </form>
   {invalidDates ? <p className="error" role="alert">La fecha inicial no puede ser posterior a la final.</p> : history.isLoading ? <Loading/> : history.isError ? <p className="error" role="alert">No pudimos cargar las ventas. <button onClick={()=>history.refetch()}>Reintentar</button></p> : !history.data?.content.length ? <div className="sales-empty"><ShoppingBag/><h2>{Object.entries(filters).some(([key,value])=>key!=='page'&&key!=='status'&&!!value)||filters.status!=='all'?'No encontramos ventas con esos filtros':'Aún no hay ventas registradas'}</h2><p>Cuando confirmes una venta por tus canales, regístrala aquí para actualizar el inventario.</p></div> : <>
    <p className="sales-count" role="status">{history.data.totalElements} {history.data.totalElements===1?'venta':'ventas'} · fechas de Colombia</p>
    <div className="table-wrap"><table className="sales-table"><thead><tr><th>Venta / fecha</th><th>Cliente</th><th>Canal / pago</th><th>Productos</th><th>Total</th><th>Estado</th><th/></tr></thead><tbody>{history.data.content.map(sale=><tr key={sale.id}><td><strong>#{sale.id}</strong><small>{date(sale.date)}</small></td><td>{sale.customer}</td><td>{channels[sale.channel]}<small>{payments[sale.paymentMethod]}</small></td><td>{sale.items.reduce((sum,line)=>sum+line.quantity,0)} unidades</td><td><strong>{money(sale.total)}</strong></td><td><span className={`sale-status ${sale.cancelled?'cancelled':'confirmed'}`}>{sale.cancelled?'Anulada':'Confirmada'}</span></td><td><button className="inventory-secondary" onClick={()=>setSelected(sale.id)} aria-label={`Ver detalle de venta ${sale.id}`}>Ver detalle</button></td></tr>)}</tbody></table></div>
    <nav className="sales-pagination" aria-label="Páginas de ventas"><button className="inventory-secondary" disabled={filters.page===0} onClick={()=>setFilters(current=>({...current,page:current.page-1}))}>Anterior</button><span>Página {filters.page+1} de {history.data.totalPages}</span><button className="inventory-secondary" disabled={filters.page+1>=history.data.totalPages} onClick={()=>setFilters(current=>({...current,page:current.page+1}))}>Siguiente</button></nav>
   </>}
  </Card>
  {creating && <NewSaleDialog busy={create.isPending} onClose={closeCreate} onSave={async payload=>{const sale=await create.mutateAsync(payload);closeCreate();setSelected(sale.id);toast.success(`Venta #${sale.id} registrada`);}}/>}
  {selected!==null && <InventoryDialog title={`Venta #${selected}`} onClose={()=>setSelected(null)}>{detail.isLoading?<Loading/>:detail.isError?<p className="error" role="alert">No pudimos consultar el detalle. <button onClick={()=>detail.refetch()}>Reintentar</button></p>:detail.data&&<SaleDetails sale={detail.data} onCancel={()=>{setCancelling(detail.data!);setSelected(null);}}/>}</InventoryDialog>}
  {cancelling && <CancelSaleDialog sale={cancelling} busy={cancel.isPending} onClose={()=>{setSelected(cancelling.id);setCancelling(null);}} onConfirm={async reason=>{const sale=await cancel.mutateAsync({id:cancelling.id,reason});setCancelling(null);setSelected(sale.id);toast.success('Venta anulada y existencias devueltas');}}/>}
 </div>;
}

type DraftLine = { product:Product; quantity:string; unitPrice:string };
function NewSaleDialog({busy,onClose,onSave}:{busy:boolean;onClose:()=>void;onSave:(payload:CreateSale)=>Promise<void>}) {
 const products = useQuery({queryKey:['sale-products'],queryFn:api.saleProducts,refetchInterval:15000,refetchOnWindowFocus:true});
 const customers = useQuery({queryKey:['customers'],queryFn:()=>api.contacts('customers')});
 const [search,setSearch] = useState('');
 const [lines,setLines] = useState<DraftLine[]>([]);
 const [customerId,setCustomerId] = useState('');
 const [paymentMethod,setPaymentMethod] = useState<PaymentMethod>('TRANSFER');
 const [channel,setChannel] = useState<SaleChannel>('WHATSAPP');
 const [discount,setDiscount] = useState('0');
 const [notes,setNotes] = useState('');
 const [error,setError] = useState('');
 const [uncertain,setUncertain] = useState(false);
 const [requestId] = useState(()=>crypto.randomUUID());
 const pendingPayload = useRef<CreateSale|null>(null);
 const locked = busy||uncertain;
 const matches = (products.data??[]).filter(product=>normalizeProductName(`${product.name} ${product.code} ${product.shade??''}`).includes(normalizeProductName(search)));
 const latest = (line:DraftLine) => products.data?.find(product=>product.id===line.product.id);
 const validLines = lines.length>0 && lines.every(line=>{const product=latest(line);return product&&!product.provisionalData&&Number.isSafeInteger(Number(line.quantity))&&Number(line.quantity)>0&&Number(line.quantity)<=product.stock&&line.unitPrice.trim()!==''&&Number.isFinite(Number(line.unitPrice))&&Number(line.unitPrice)>=0&&Math.abs(Number(line.unitPrice)*100-Math.round(Number(line.unitPrice)*100))<0.000001;});
 const subtotalCents = lines.reduce((sum,line)=>sum+Math.round(Number(line.unitPrice||0)*100)*Number(line.quantity||0),0);
 const discountCents = Math.round(Number(discount||0)*100);
 const validDiscount = Number.isFinite(discountCents)&&discountCents>=0&&discountCents<=subtotalCents;
 function add(product:Product) {
  setError('');setLines(current=>current.some(line=>line.product.id===product.id)?current.map(line=>line.product.id===product.id?{...line,quantity:String(Math.min(Number(line.quantity||0)+1,product.stock))}:line):[...current,{product,quantity:'1',unitPrice:String(product.salePrice)}]);
 }
 function update(index:number,key:'quantity'|'unitPrice',value:string){setLines(current=>current.map((line,i)=>i===index?{...line,[key]:value}:line));}
 async function submit(event:FormEvent) {
  event.preventDefault();if(busy)return;
  if(!pendingPayload.current){if(!validLines||!validDiscount||products.isError){setError('Revisa productos, cantidades, precios y descuento antes de registrar.');return;}pendingPayload.current={requestId,customerId:customerId?Number(customerId):null,paymentMethod,channel,discount:discountCents/100,notes:notes.trim(),items:lines.map(line=>({productId:line.product.id,quantity:Number(line.quantity),unitPrice:Number(line.unitPrice)}))};}
  setError('');
  try{await onSave(pendingPayload.current);}catch(failure){setError(errorMessage(failure));const code=(failure as {code?:string})?.code;if(['BUSINESS_ERROR','VALIDATION_ERROR','NOT_FOUND'].includes(code??'')){pendingPayload.current=null;setUncertain(false);products.refetch();}else setUncertain(true);}
 }
 return <InventoryDialog title="Registrar venta" busy={busy} onClose={onClose}><form className="sale-form" onSubmit={submit}>
  <p>Registra una venta ya confirmada. Al guardar se descuentan las unidades; seleccionar productos no las reserva.</p>
  <fieldset disabled={locked}><div className="sale-form-grid">
   <label>Canal de venta<select name="channel" value={channel} onChange={event=>setChannel(event.target.value as SaleChannel)}>{Object.entries(channels).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>
   <label>Pago recibido<select name="paymentMethod" value={paymentMethod} onChange={event=>setPaymentMethod(event.target.value as PaymentMethod)}>{Object.entries(payments).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>
   <label className="sale-wide">Cliente (opcional)<select name="customerId" value={customerId} onChange={event=>setCustomerId(event.target.value)} disabled={customers.isLoading||customers.isError}><option value="">Consumidor final</option>{customers.data?.filter(customer=>customer.active).map(customer=><option key={customer.id} value={customer.id}>{customer.name}</option>)}</select></label>
  </div></fieldset>
  {customers.isError&&<p className="sales-hint" role="status">No pudimos cargar clientes. Puedes registrar como consumidor final. <button type="button" onClick={()=>customers.refetch()}>Reintentar</button></p>}
  <fieldset disabled={locked}><legend>Productos y tonalidades</legend><label>Buscar en inventario<input type="search" value={search} onChange={event=>setSearch(event.target.value)} placeholder="Nombre, código o tono"/></label>
   {products.isLoading?<Loading/>:products.isError?<p className="error" role="alert">No pudimos actualizar el inventario. <button type="button" onClick={()=>products.refetch()}>Reintentar</button></p>:<div className="sale-product-picker" aria-label="Productos disponibles">{matches.length===0?<p>No encontramos productos.</p>:matches.map(product=><button type="button" key={product.id} disabled={product.stock<=0||product.provisionalData||(Number(lines.find(line=>line.product.id===product.id)?.quantity??0)>=product.stock)} onClick={()=>add(product)}><span><strong>{product.name}{product.shade?` · ${product.shade}`:''}</strong><small>{product.code}{!product.active?' · Oculto en tienda':''}</small></span><span>{money(product.salePrice)}<small>{product.provisionalData?'Datos provisionales':`${product.stock} disponibles`}</small></span><Plus size={18}/></button>)}</div>}
   <div className="sale-draft-lines">{lines.map((line,index)=>{const product=latest(line);return <div className="sale-draft-line" key={line.product.id}><div className="sale-wide"><strong>{line.product.name}{line.product.shade?` · ${line.product.shade}`:''}</strong><small>{line.product.code}</small>{!product||Number(line.quantity)>product.stock?<p className="error">La cantidad supera la disponibilidad actual. Revisa esta línea.</p>:product.costConfirmed===false?<p className="sales-hint">Costo sin confirmar; el margen quedará pendiente.</p>:null}</div><label>Cantidad<input name={`quantity-${line.product.id}`} type="number" min="1" max={product?.stock??0} step="1" required value={line.quantity} onChange={event=>update(index,'quantity',event.target.value)}/></label><label>Precio por unidad (COP)<input name={`price-${line.product.id}`} type="number" min="0" max="999999999999.99" step="0.01" required value={line.unitPrice} onChange={event=>update(index,'unitPrice',event.target.value)}/></label><div className="sale-line-total"><strong>{money(Math.round(Number(line.unitPrice||0)*100)*Number(line.quantity||0)/100)}</strong><button type="button" className="icon-button danger-text" aria-label={`Quitar ${line.product.name}${line.product.shade?` ${line.product.shade}`:''}`} onClick={()=>setLines(current=>current.filter((_,i)=>i!==index))}><Trash2 size={18}/></button></div></div>;})}</div>
   {lines.length===0&&<p className="sales-hint">Selecciona al menos un producto para preparar la venta.</p>}
   <div className="sale-form-grid"><label>Descuento total (COP)<input name="discount" type="number" min="0" max={subtotalCents/100} step="0.01" value={discount} onChange={event=>setDiscount(event.target.value)} required/></label><label className="sale-wide">Notas (opcional)<textarea name="notes" maxLength={500} rows={2} value={notes} onChange={event=>setNotes(event.target.value)} placeholder="Referencia de la conversación o información de la venta"/></label></div>
  </fieldset>
  <div className="sale-totals"><span>Subtotal<strong>{money(subtotalCents/100)}</strong></span><span>Descuento<strong>{money(discountCents/100)}</strong></span><span>Total registrado<strong>{money(Math.max(0,subtotalCents-discountCents)/100)}</strong></span></div>
  {!validDiscount&&<p className="error" role="alert">El descuento no puede superar el subtotal.</p>}
  {error&&<p className="error" role="alert">{error}</p>}
  {uncertain&&<p className="sales-hint" role="alert">No pudimos confirmar la respuesta. Reintenta con los mismos datos: si la venta ya se guardó, se recuperará sin volver a descontar stock. También puedes cerrar y revisar el historial antes de registrar otra venta.</p>}
  <footer><button type="button" className="inventory-secondary" onClick={onClose} disabled={busy}>Cerrar</button><button type="submit" className="primary" disabled={busy||(!uncertain&&(!validLines||!validDiscount||products.isError))}>{busy?'Registrando…':uncertain?'Reintentar la misma venta':'Confirmar y registrar venta'}</button></footer>
 </form></InventoryDialog>;
}

function SaleDetails({sale,onCancel}:{sale:Sale;onCancel:()=>void}) {
 return <div className="sale-detail"><span className={`sale-status ${sale.cancelled?'cancelled':'confirmed'}`}>{sale.cancelled?'Anulada':'Confirmada'}</span><dl><dt>Fecha en Colombia</dt><dd>{date(sale.date)}</dd><dt>Cliente</dt><dd>{sale.customer}</dd><dt>Canal</dt><dd>{channels[sale.channel]}</dd><dt>Pago recibido</dt><dd>{payments[sale.paymentMethod]}</dd><dt>Registrada por</dt><dd>{sale.recordedBy||'Sin registro histórico'}</dd></dl>
  <div className="table-wrap"><table className="sales-table"><thead><tr><th>Producto / tono</th><th>Cant.</th><th>Precio/u</th><th>Costo/u</th><th>Subtotal</th></tr></thead><tbody>{sale.items.map((item,index)=><tr key={`${item.productId}-${index}`}><td><strong>{item.product}</strong><small>{item.shade}{item.shade?' · ':''}{item.code}</small></td><td>{item.quantity}</td><td>{money(item.unitPrice)}</td><td>{item.unitCost===null?'Sin confirmar':money(item.unitCost)}</td><td>{money(item.subtotal)}</td></tr>)}</tbody></table></div>
  <div className="sale-totals"><span>Subtotal<strong>{money(sale.subtotal)}</strong></span><span>Descuento<strong>{money(sale.discount)}</strong></span><span>Total registrado<strong>{money(sale.total)}</strong></span><span>Costo de productos<strong>{sale.costTotal===null?'Sin confirmar':money(sale.costTotal)}</strong></span><span>Margen de productos<strong>{sale.grossProfit===null?'Sin confirmar':money(sale.grossProfit)}</strong></span></div>
  <p className="sales-hint">El margen es el total menos el costo de estos productos; no descuenta gastos del negocio.</p>
  {!sale.historicalSnapshot&&<p className="sales-hint">Venta anterior al registro de datos históricos: algunos nombres corresponden al producto actual. Los costos desconocidos no se reconstruyen.</p>}
  {sale.notes&&<div><h3>Notas</h3><p className="sale-notes">{sale.notes}</p></div>}
  {sale.cancelled?<div className="sale-cancelled-note"><strong>Anulación registrada</strong><p className="sale-notes">{sale.cancellationReason}</p><small>{sale.cancelledAt?date(sale.cancelledAt):''} · {sale.cancelledBy||'Sin registro'}</small><p>Las unidades se devolvieron al inventario. Esta venta se excluye de los totales de ventas confirmadas.</p></div>:<footer><button type="button" className="inventory-delete" onClick={onCancel}>Anular venta</button></footer>}
 </div>;
}

function CancelSaleDialog({sale,busy,onClose,onConfirm}:{sale:Sale;busy:boolean;onClose:()=>void;onConfirm:(reason:string)=>Promise<void>}) {
 const [reason,setReason]=useState('');const [error,setError]=useState('');
 async function submit(event:FormEvent){event.preventDefault();if(busy)return;if(reason.trim().length<5){setError('Explica el motivo con al menos 5 caracteres.');return;}setError('');try{await onConfirm(reason.trim());}catch(failure){setError(errorMessage(failure));}}
 return <InventoryDialog title={`Anular venta #${sale.id}`} busy={busy} onClose={onClose}><form className="sale-form" onSubmit={submit}><p>Se devolverán todas las unidades de esta venta al inventario y se conservará su historial. Esta acción no realiza reembolsos de dinero.</p><label>Motivo de anulación<textarea autoFocus name="reason" minLength={5} maxLength={500} rows={3} required disabled={busy} value={reason} onChange={event=>setReason(event.target.value)}/></label>{error&&<p className="error" role="alert">{error}</p>}<footer><button type="button" className="inventory-secondary" disabled={busy} onClick={onClose}>Volver al detalle</button><button className="inventory-delete" disabled={busy}>{busy?'Anulando…':'Confirmar anulación'}</button></footer></form></InventoryDialog>;
}
