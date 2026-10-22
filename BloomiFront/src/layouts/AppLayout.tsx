import { useState, type ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { BarChart3, Boxes, ChevronLeft, ChevronRight, LogOut, Menu, PackagePlus, ReceiptText, Search, ShoppingBag, Sparkles, Tags, Truck, UsersRound, X } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import '../styles/admin-layout.css';

const groups = [
 ['GENERAL', [['/admin', 'Dashboard', BarChart3]]],
 ['OPERACIÓN', [['/admin/inventario', 'Inventario', Boxes], ['/admin/ventas', 'Ventas', ShoppingBag], ['/admin/compras', 'Compras', PackagePlus], ['/admin/gastos', 'Gastos', ReceiptText]]],
 ['CATÁLOGOS', [['/admin/categorias', 'Categorías', Tags], ['/admin/marcas', 'Marcas', Sparkles], ['/admin/proveedores', 'Proveedores', Truck], ['/admin/clientes', 'Clientes', UsersRound]]],
] as const;

export default function AppLayout({ children }: { children: ReactNode }) {
 const [compact, setCompact] = useState(false);
 const [mobile, setMobile] = useState(false);
 const [search,setSearch]=useState('');
 const navigate=useNavigate();
 const { session, logout } = useAuth();
 return <div className={`shell ${compact ? 'compact' : ''}`}>
  <aside id="admin-navigation" className={mobile ? 'open' : ''}>
   <div className="brand"><div className="brand-mark">B</div><div><strong>Bloomi Bunny</strong><span>Administración</span></div></div>
   <button className="admin-nav-close" aria-label="Cerrar navegación" onClick={() => setMobile(false)}><X/></button>
   <NavLink to="/" className="store-return">Ver tienda</NavLink>
   <nav aria-label="Administración">{groups.map(([title, items]) => <div className="nav-group" key={title}><small>{title}</small>{items.map(([to, label, Icon]) => <NavLink to={to} key={to} end={to === '/admin'} onClick={() => setMobile(false)}><Icon/><span>{label}</span></NavLink>)}</div>)}</nav>
   <div className="profile"><div className="avatar">{session?.username[0]?.toUpperCase()}</div><div><strong>{session?.username}</strong><span>Administradora</span></div><button onClick={logout} title="Cerrar sesión"><LogOut/></button></div>
   <button className="collapse" onClick={() => setCompact(!compact)} aria-label={compact ? 'Expandir menú' : 'Contraer menú'}>{compact ? <ChevronRight/> : <ChevronLeft/>}</button>
  </aside>
  {mobile && <button className="admin-nav-backdrop" aria-label="Cerrar menú de administración" onClick={() => setMobile(false)}/>}
  <main><div className="topbar"><button className="mobile-menu" aria-label="Abrir navegación" aria-expanded={mobile} aria-controls="admin-navigation" onClick={() => setMobile(!mobile)}><Menu/></button><form className="global-search" role="search" onSubmit={event=>{event.preventDefault();navigate(`/admin/inventario${search.trim()?`?q=${encodeURIComponent(search.trim())}`:''}`);setSearch('');}}><Search aria-hidden="true"/><input aria-label="Buscar productos en inventario" type="search" value={search} onChange={event=>setSearch(event.target.value)} placeholder="Buscar productos y pulsar Enter…"/></form><span>Administración</span></div><div className="content">{children}</div></main>
 </div>;
}
