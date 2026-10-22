import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowDownRight, ArrowUpRight, Boxes, CircleDollarSign, PackageX, RefreshCw, ShoppingBag, TriangleAlert } from 'lucide-react';
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useDashboard } from '../../hooks/useApi';
import { Card, Loading, PageHeader } from '../common/Ui';
import type { DashboardPeriod } from '../../types';
import '../../styles/dashboard.css';

const money=(value:number)=>new Intl.NumberFormat('es-CO',{style:'currency',currency:'COP',minimumFractionDigits:0,maximumFractionDigits:2}).format(value);
const labelDate=(value:string,monthly=false)=>new Date(`${value}T12:00:00-05:00`).toLocaleDateString('es-CO',{timeZone:'America/Bogota',...(monthly?{month:'short',year:'numeric'}:{day:'numeric',month:'short'})});
const time=(value:string)=>new Date(value).toLocaleString('es-CO',{timeZone:'America/Bogota',dateStyle:'medium',timeStyle:'short'});
function salesLink(from:string,to:string,q=''){const params=new URLSearchParams({from,to,status:'confirmed'});if(q)params.set('q',q);return `/admin/ventas?${params}`;}
const inventoryLink=(code:string)=>`/admin/inventario?q=${encodeURIComponent(code)}`;
const movementLabels:Record<string,string>={SALE:'Venta',PURCHASE:'Compra',ADJUSTMENT_IN:'Entrada por ajuste',ADJUSTMENT_OUT:'Salida por ajuste',EXPENSE:'Gasto',INCOME:'Ingreso'};

export default function DashboardOverview(){
 const dashboard=useDashboard();
 const [period,setPeriod]=useState<'daily'|'monthly'>('daily');
 if(dashboard.isLoading)return <Loading/>;
 if(dashboard.isError||!dashboard.data)return <div className="dashboard-page"><PageHeader eyebrow="RESUMEN GENERAL" title="Resumen del negocio" subtitle="Ventas e inventario de Bloomi Bunny."/><Card><div className="dashboard-error" role="alert"><TriangleAlert/><h2>No pudimos actualizar el resumen</h2><p>{(dashboard.error as {message?:string})?.message??'Revisa la conexión y vuelve a intentarlo.'}</p><button className="primary" onClick={()=>dashboard.refetch()}>Reintentar</button></div></Card></div>;
 const data=dashboard.data;
 const monthStart=`${data.today.slice(0,7)}-01`;
 const monthLink=salesLink(monthStart,data.today);
 const points=period==='daily'?data.dailySales:data.monthlySales;
 const hasSales=points.some(point=>point.count>0);
 const maxUnits=Math.max(1,...data.topProducts.map(product=>product.units));
 return <div className="dashboard-page">
  <PageHeader eyebrow="RESUMEN GENERAL" title="Resumen del negocio" subtitle="Ventas confirmadas e inventario, con fechas de Colombia." action={<Link className="primary" to="/admin/ventas?new=1"><ShoppingBag size={18}/> Registrar venta</Link>}/>
  <div className="dashboard-updated"><span>Actualizado: {time(data.generatedAt)} · hora de Colombia</span><button type="button" disabled={dashboard.isFetching} onClick={()=>dashboard.refetch()}><RefreshCw size={15} className={dashboard.isFetching?'spin':''}/>{dashboard.isFetching?'Actualizando…':'Actualizar'}</button></div>
  <div className="dashboard-stats">
   <Card><span className="dashboard-stat-label">Ventas de hoy</span><strong>{money(data.salesToday)}</strong><small>{data.salesTodayCount} {data.salesTodayCount===1?'venta confirmada':'ventas confirmadas'}</small><Link to={salesLink(data.today,data.today)}>Ver ventas de hoy →</Link></Card>
   <Card><span className="dashboard-stat-label">Ventas del mes</span><strong>{money(data.salesMonth)}</strong><small>{data.salesMonthCount} {data.salesMonthCount===1?'venta confirmada':'ventas confirmadas'}</small><Link to={monthLink}>Ver ventas del mes →</Link></Card>
   <Card><span className="dashboard-stat-label">Costo vendido del mes</span><strong>{data.costMonth===null?'Por confirmar':money(data.costMonth)}</strong><small>{data.costMonth===null?`${money(data.knownCostMonth)} con costo registrado`:'Costo histórico de los productos vendidos'}</small><Link to={monthLink}>Revisar ventas y costos →</Link></Card>
   <Card><span className="dashboard-stat-label">Margen de ventas del mes</span><strong>{data.grossProfit===null?'Por confirmar':money(data.grossProfit)}</strong><small>Total vendido menos costo histórico</small><Link to={monthLink}>Consultar detalle →</Link></Card>
  </div>
  <p className="dashboard-explanation"><CircleDollarSign size={16}/> Los importes incluyen descuentos y excluyen ventas anuladas. El margen de productos no descuenta gastos del negocio.</p>
  {data.unknownCostSales>0&&<div className="dashboard-cost-notice" role="status"><TriangleAlert size={20}/><div><strong>Hay costos históricos sin confirmar</strong><p>{data.unknownCostSales} {data.unknownCostSales===1?'venta del mes contiene':'ventas del mes contienen'} {data.unknownCostUnits} {data.unknownCostUnits===1?'unidad sin costo registrado':'unidades sin costo registrado'}. El costo completo y el margen quedan por confirmar.</p><Link to={monthLink}>Revisar ventas del mes →</Link></div></div>}
  <div className="dashboard-alerts">
   <Link to="/admin/inventario?stock=low"><TriangleAlert/><div><strong>{data.lowStock} {data.lowStock===1?'referencia':'referencias'} con stock bajo</strong><span>Ver stock bajo →</span></div></Link>
   <Link to="/admin/inventario?stock=out"><PackageX/><div><strong>{data.outOfStock} {data.outOfStock===1?'referencia agotada':'referencias agotadas'}</strong><span>Ver agotados →</span></div></Link>
   <Link to="/admin/inventario"><Boxes/><div><strong>{data.totalProducts} referencias en inventario</strong><span>{data.activeProducts} publicadas en la tienda · ver inventario →</span></div></Link>
  </div>
  <div className="dashboard-charts">
   <Card className="dashboard-chart"><div className="dashboard-card-title"><div><h2>Ventas por fecha</h2><p>{period==='daily'?'Últimos 30 días':'Últimos 7 meses, hasta hoy'} · Colombia</p></div><div className="dashboard-period" role="group" aria-label="Periodo del gráfico"><button aria-pressed={period==='daily'} onClick={()=>setPeriod('daily')}>Días</button><button aria-pressed={period==='monthly'} onClick={()=>setPeriod('monthly')}>Meses</button></div></div>
    {hasSales?<div className="dashboard-chart-area" role="img" aria-label={`Ventas reales de los ${period==='daily'?'últimos 30 días':'últimos 7 meses'}. Las cifras están disponibles en la tabla del gráfico.`}><ResponsiveContainer width="100%" height={280}><AreaChart data={points} accessibilityLayer margin={{left:0,right:12,top:12,bottom:4}}><defs><linearGradient id="dashboard-rose" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#b76e79" stopOpacity={0.3}/><stop offset="100%" stopColor="#b76e79" stopOpacity={0.02}/></linearGradient></defs><CartesianGrid strokeDasharray="3 3" vertical={false}/><XAxis dataKey="date" tickFormatter={value=>labelDate(String(value),period==='monthly')} minTickGap={25}/><YAxis width={62} tickFormatter={value=>new Intl.NumberFormat('es-CO',{notation:'compact',maximumFractionDigits:1}).format(Number(value))}/><Tooltip labelFormatter={value=>labelDate(String(value),period==='monthly')} formatter={value=>[money(Number(value)),'Ventas']}/><Area type="monotone" dataKey="total" stroke="#a45665" strokeWidth={3} fill="url(#dashboard-rose)"/></AreaChart></ResponsiveContainer></div>:<div className="dashboard-empty"><ShoppingBag/><h3>Aún no hay ventas confirmadas en este periodo</h3><p>El gráfico mostrará los registros reales cuando confirmes una venta.</p></div>}
    <Link className="dashboard-text-link" to={salesLink(points[0]?.date??monthStart,data.today)}>Ver ventas de este periodo →</Link>
    <details className="dashboard-chart-table"><summary>Ver cifras del gráfico</summary><PeriodTable points={points} monthly={period==='monthly'}/></details>
   </Card>
   <Card><div className="dashboard-card-title"><div><h2>Productos más vendidos</h2><p>Este mes · por unidades y tonalidad</p></div></div>{data.topProducts.length===0?<div className="dashboard-empty"><Boxes/><h3>Aún no hay productos vendidos este mes</h3><p>Se incluirán aquí las unidades de ventas confirmadas.</p></div>:<ol className="dashboard-best-sellers">{data.topProducts.map((product,index)=><li key={product.id}><Link to={salesLink(monthStart,data.today,product.code)}><span className="dashboard-rank">{index+1}</span><span className="dashboard-product-name"><strong>{product.product}</strong>{product.shade&&<small>{product.shade}</small>}<small>{product.code}</small><span className="dashboard-product-bar" aria-hidden="true"><i style={{width:`${product.units/maxUnits*100}%`}}/></span></span><b>{product.units}<small>{product.units===1?'unidad':'unidades'}</small></b></Link></li>)}</ol>}</Card>
  </div>
  <div className="dashboard-lists">
   <Card><div className="dashboard-card-title"><div><h2>Referencias que necesitan atención</h2><p>Agotadas y por debajo del mínimo de inventario.</p></div><Link to="/admin/inventario">Ver inventario →</Link></div>{data.stockAlerts.length===0?<p className="dashboard-empty-text">No hay referencias agotadas ni con stock bajo.</p>:<ul className="dashboard-stock-list">{data.stockAlerts.map(product=><li key={product.id}><Link to={inventoryLink(product.code)}><span><strong>{product.product}</strong><small>{product.shade?`${product.shade} · `:''}{product.code}{!product.active?' · Oculto en tienda':''}</small></span><span className={`dashboard-stock-badge ${product.stock===0?'out':'low'}`}>{product.stock===0?'Agotado':`${product.stock} uds. · mínimo ${product.minimumStock}`}</span></Link></li>)}</ul>}{data.lowStock+data.outOfStock>data.stockAlerts.length&&<p className="dashboard-list-note">Se muestran {data.stockAlerts.length} de {data.lowStock+data.outOfStock} referencias que necesitan atención. Usa los filtros de inventario para revisar todas.</p>}</Card>
   <Card><div className="dashboard-card-title"><div><h2>Últimos movimientos</h2><p>Actividad real del inventario · hora de Colombia</p></div><Link to="/admin/ventas">Historial de ventas →</Link></div>{data.recentMovements.length===0?<p className="dashboard-empty-text">Aún no hay movimientos registrados.</p>:<ul className="dashboard-movement-list">{data.recentMovements.map((movement,index)=><li key={`${movement.date}-${index}`}><span className={`dashboard-movement-icon ${movement.type==='SALE'||movement.type==='ADJUSTMENT_OUT'?'out':'in'}`}>{movement.type==='SALE'||movement.type==='ADJUSTMENT_OUT'?<ArrowDownRight size={18}/>:<ArrowUpRight size={18}/>}</span><div>{movement.deleted?<strong>{movement.product}</strong>:<Link to={inventoryLink(movement.code)}><strong>{movement.product}</strong></Link>}<small>{movement.shade?`${movement.shade} · `:''}{movementLabels[movement.type]??movement.type} · {movement.quantity} uds.</small>{movement.reference&&<small>{movement.reference}</small>}<time dateTime={movement.date}>{time(movement.date)}</time></div><b>Stock {movement.resultingStock}</b></li>)}</ul>}</Card>
  </div>
  <footer className="dashboard-footer"><span>{data.customers} contactos de clientes registrados</span><Link to="/admin/clientes">Ver clientes →</Link><Link to="/admin/ventas">Consultar historial de ventas →</Link></footer>
 </div>;
}

function PeriodTable({points,monthly}:{points:DashboardPeriod[];monthly:boolean}){return <div className="table-wrap"><table><thead><tr><th>{monthly?'Mes':'Fecha'}</th><th>Ventas confirmadas</th><th>Importe</th></tr></thead><tbody>{points.map(point=><tr key={point.date}><td>{labelDate(point.date,monthly)}</td><td>{point.count}</td><td>{money(point.total)}</td></tr>)}</tbody></table></div>;}
