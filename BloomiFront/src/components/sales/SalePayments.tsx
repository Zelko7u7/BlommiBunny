import { useRef, useState, type FormEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { api } from '../../services/api';
import type { CreateSalePayment, PaymentMethod, Sale } from '../../types';

const money=(value:number)=>new Intl.NumberFormat('es-CO',{style:'currency',currency:'COP',maximumFractionDigits:2}).format(value);
const methods:Record<PaymentMethod,string>={CASH:'Efectivo',TRANSFER:'Transferencia',CARD:'Tarjeta',OTHER:'Otro'};
const labels={UNREVIEWED:'Pagos por revisar',PENDING:'Pendiente de pago',PARTIAL:'Pago parcial',PAID:'Pagada · sin autorizar'};
const time=(value:string)=>new Date(value).toLocaleString('es-CO',{timeZone:'America/Bogota',dateStyle:'medium',timeStyle:'short'});
const today=()=>new Intl.DateTimeFormat('en-CA',{timeZone:'America/Bogota',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
const errorText=(error:unknown)=>(error as {message?:string})?.message??'No pudimos completar la operación. Reintenta.';

export function PaymentStatus({sale}:{sale:Sale}){
 return <span className={`sale-status ${sale.cancelled?'cancelled':sale.collectionAuthorized?'confirmed':'pending'}`}>{sale.cancelled?'Anulada':sale.collectionAuthorized?'Cobro autorizado':labels[sale.paymentStatus]??'Actualiza el backend'}</span>;
}

export default function SalePayments({sale}:{sale:Sale}){
 const qc=useQueryClient();
 const [adding,setAdding]=useState(false);
 const [amount,setAmount]=useState('');
 const [paymentDate,setPaymentDate]=useState(today);
 const [method,setMethod]=useState<PaymentMethod>('TRANSFER');
 const [reference,setReference]=useState('');
 const [notes,setNotes]=useState('');
 const [busy,setBusy]=useState(false);
 const [error,setError]=useState('');
 const [uncertain,setUncertain]=useState(false);
 const pending=useRef<CreateSalePayment|null>(null);
 const [action,setAction]=useState<{type:'cancel';id:number}|{type:'collection';authorized:boolean}|{type:'review'}|null>(null);
 const [reason,setReason]=useState('');
 const actionVersion=useRef(sale.version);
 const locked=busy||uncertain;
 async function updated(result:Sale){
  qc.setQueryData(['sale',sale.id],result);
  await Promise.all(['sales','dashboard'].map(key=>qc.invalidateQueries({queryKey:[key]})));
 }
 async function add(event:FormEvent){
  event.preventDefault();if(busy)return;setError('');
  if(!pending.current){
   const value=Number(amount);
   if(!amount.trim()||!Number.isFinite(value)||value<=0||value>(sale.balance??0)||Math.abs(value*100-Math.round(value*100))>0.000001){setError('Indica un abono positivo, de hasta dos decimales y que no supere el saldo.');return;}
   pending.current={requestId:crypto.randomUUID(),amount:Math.round(value*100)/100,date:paymentDate,paymentMethod:method,reference:reference.trim(),notes:notes.trim()};
  }
  setBusy(true);
  try{
   const result=await api.addSalePayment(sale.id,pending.current);
   await updated(result);pending.current=null;setUncertain(false);setAdding(false);setAmount('');setReference('');setNotes('');toast.success('Abono registrado. La autorización del cobro es independiente.');
  }catch(failure){
   setError(errorText(failure));
   if(['BUSINESS_ERROR','VALIDATION_ERROR','NOT_FOUND'].includes((failure as {code?:string}).code??'')){pending.current=null;setUncertain(false);await qc.invalidateQueries({queryKey:['sale',sale.id]});}
   else setUncertain(true);
  }finally{setBusy(false);}
 }
 function choose(next:typeof action){actionVersion.current=sale.version;setAction(next);setReason('');setError('');}
 async function confirm(event:FormEvent){
  event.preventDefault();if(busy||!action)return;setError('');setBusy(true);
  try{
   const result=action.type==='cancel'?await api.cancelSalePayment(sale.id,action.id,reason.trim()):action.type==='review'?await api.reviewSalePayments(sale.id,actionVersion.current,reason.trim()):await api.authorizeSaleCollection(sale.id,actionVersion.current,action.authorized,reason.trim());
   await updated(result);setAction(null);setReason('');toast.success('Cambio registrado en el historial');
  }catch(failure){setError(errorText(failure));await qc.invalidateQueries({queryKey:['sale',sale.id]});}finally{setBusy(false);}
 }
 if(!Array.isArray(sale.payments))return <p className="error" role="alert">Reinicia el backend para gestionar abonos.</p>;
 return <section className="sale-payments" aria-label="Abonos y cobro">
  <h3>Abonos y saldo</h3><PaymentStatus sale={sale}/>
  <div className="sale-totals"><span>Importe de la venta<strong>{money(sale.total)}</strong></span><span>Abonos registrados<strong>{sale.paidTotal===null?'Por revisar':money(sale.paidTotal)}</strong></span><span>{sale.cancelled?'Saldo histórico al anular':'Saldo pendiente'}<strong>{sale.balance===null?'Por revisar':money(sale.balance)}</strong></span></div>
  <p className="sales-hint">Los abonos no suman la venta completa a los cobros confirmados. Para incluirla debes completar el pago y autorizar el cobro.</p>
  {sale.cancelled&&<p className="sale-cancelled-note">Los abonos se conservan como historial y se excluyen de los indicadores. Anular la venta no devuelve el dinero; gestiona cualquier reembolso por separado.</p>}
  {!sale.cancelled&&!sale.paymentsReviewed&&<div className="sale-cancelled-note"><p>Esta venta es anterior al registro de abonos. No se han supuesto pagos ni deudas. Revisa los comprobantes antes de iniciar el seguimiento y registra después cada pago real.</p><button type="button" className="inventory-secondary" disabled={busy} onClick={()=>choose({type:'review'})}>Iniciar seguimiento de pagos</button></div>}
  {!sale.cancelled&&sale.paymentsReviewed&&<>
   {!adding&&!action&&(sale.balance??0)>0&&<button type="button" className="primary" onClick={()=>{setAdding(true);setError('');setPaymentDate(today());}}>Registrar abono</button>}
   {!adding&&<label className="collection-control">Incluir en cobros confirmados<select aria-label="Autorización del cobro" value={sale.collectionAuthorized?'authorized':'pending'} disabled={busy||!!action} onChange={event=>choose({type:'collection',authorized:event.target.value==='authorized'})}><option value="pending">Sin autorizar</option><option value="authorized" disabled={sale.balance!==0}>Autorizar cobro completo</option></select></label>}
   {sale.collectionAuthorizedAt&&<p className="sales-hint">Autorizado por {sale.collectionAuthorizedBy} · {time(sale.collectionAuthorizedAt)}. Se incluye en el periodo de autorización.</p>}
  </>}
  {adding&&<form className="sale-form payment-entry" onSubmit={add}>
   <h4>Registrar abono</h4><fieldset disabled={locked}><div className="sale-form-grid">
    <label>Importe (COP)<input autoFocus aria-label="Importe del abono" type="number" required min="0.01" max={sale.balance??0} step="0.01" value={amount} onChange={event=>setAmount(event.target.value)}/></label>
    <label>Fecha del pago<input type="date" required max={today()} min={new Intl.DateTimeFormat('en-CA',{timeZone:'America/Bogota',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(sale.date))} value={paymentDate} onChange={event=>setPaymentDate(event.target.value)}/></label>
    <label>Medio de pago<select value={method} onChange={event=>setMethod(event.target.value as PaymentMethod)}>{Object.entries(methods).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>
    <label>Referencia (opcional)<input maxLength={120} value={reference} onChange={event=>setReference(event.target.value)}/></label>
    <label className="sale-wide">Notas (opcional)<textarea maxLength={500} rows={2} value={notes} onChange={event=>setNotes(event.target.value)}/></label>
   </div></fieldset>
   {uncertain&&<p className="sales-hint" role="alert">La respuesta no se confirmó. Reintenta el mismo abono: se recuperará sin duplicarlo. Si cierras, revisa el historial antes de crear otro.</p>}
   <div className="payment-actions"><button type="button" className="inventory-secondary" disabled={busy} onClick={()=>{setAdding(false);setUncertain(false);pending.current=null;setError('');}}>Cerrar formulario</button><button className="primary" disabled={busy}>{busy?'Guardando…':uncertain?'Reintentar el mismo abono':'Guardar abono'}</button></div>
  </form>}
  {action&&<form className="sale-form payment-entry" onSubmit={confirm}>
   <h4>{action.type==='cancel'?'Anular abono':action.type==='review'?'Iniciar seguimiento':action.authorized?'Autorizar cobro completo':'Retirar autorización'}</h4>
   <p>{action.type==='cancel'?'El abono quedará anulado, se recalculará el saldo y se retirará cualquier autorización de cobro. No cambia el inventario ni devuelve dinero.':action.type==='review'?'Confirma que revisaste esta venta. El seguimiento comienza sin abonos; registra a continuación únicamente los pagos reales comprobados.':'El cambio quedará auditado. Los cobros confirmados se agrupan por la fecha de autorización; no modifican las ventas ni sus costos históricos.'}</p>
   <label>Motivo<textarea autoFocus required minLength={5} maxLength={500} rows={2} disabled={busy} value={reason} onChange={event=>setReason(event.target.value)}/></label>
   <div className="payment-actions"><button type="button" className="inventory-secondary" disabled={busy} onClick={()=>choose(null)}>Volver</button><button className="primary" disabled={busy||reason.trim().length<5}>{busy?'Guardando…':'Confirmar cambio'}</button></div>
  </form>}
  {error&&<p className="error" role="alert">{error}</p>}
  <h4>Historial de abonos</h4>
  {!sale.payments.length?<p className="sales-hint">Sin abonos registrados.</p>:<div className="table-wrap"><table className="sales-table"><thead><tr><th>Fecha / responsable</th><th>Importe / medio</th><th>Referencia / notas</th><th>Estado</th><th/></tr></thead><tbody>{sale.payments.map(payment=><tr key={payment.id}><td>{payment.date}<small>{payment.recordedBy} · registrado {time(payment.recordedAt)}</small></td><td>{money(payment.amount)}<small>{methods[payment.paymentMethod]}</small></td><td className="sale-notes">{payment.reference||'—'}{payment.notes&&<small>{payment.notes}</small>}</td><td>{payment.cancelled?<><span className="sale-status cancelled">Anulado</span><small className="sale-notes">{payment.cancellationReason} · {payment.cancelledBy}{payment.cancelledAt?` · ${time(payment.cancelledAt)}`:''}</small></>:<span className="sale-status confirmed">Registrado</span>}</td><td>{!sale.cancelled&&!payment.cancelled&&<button type="button" className="inventory-secondary" disabled={busy||adding||!!action} onClick={()=>choose({type:'cancel',id:payment.id})}>Anular abono #{payment.id}</button>}</td></tr>)}</tbody></table></div>}
  {!!sale.collectionEvents.length&&<details className="collection-history"><summary>Historial de autorizaciones y revisiones</summary><ul>{sale.collectionEvents.map(event=><li key={event.id}><strong>{({AUTHORIZED:'Cobro autorizado',REVOKED:'Autorización retirada',REVIEWED:'Pagos revisados'} as Record<string,string>)[event.action]??event.action}</strong><p className="sale-notes">{event.reason}</p><small>{event.recordedBy} · {time(event.date)}</small></li>)}</ul></details>}
 </section>;
}
