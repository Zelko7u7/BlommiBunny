import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { useAuth } from './contexts/AuthContext';
import AppLayout from './layouts/AppLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import InventoryPage from './pages/InventoryPage';
import { NamedCatalog, ContactsPage } from './pages/CatalogPage';
import PlaceholderPage from './pages/PlaceholderPage';
import StorePage from './pages/StorePage';

function AdminOnly() {
  const { session } = useAuth();
  if (!session) return <Navigate to="/login" replace/>;
  if (session.role !== 'ADMIN') return <Navigate to="/" replace/>;
  return <AppLayout><Outlet/></AppLayout>;
}

export default function App() {
  const { session } = useAuth();
  return <Routes>
    <Route path="/" element={<StorePage/>}/>
    <Route path="/producto/:id" element={<StorePage/>}/>
    <Route path="/favoritos" element={<StorePage/>}/>
    <Route path="/nosotros" element={<StorePage/>}/>
    <Route path="/politicas/:slug" element={<StorePage/>}/>
    <Route path="/login" element={session ? <Navigate to={session.role === 'ADMIN' ? '/admin' : '/'} replace/> : <LoginPage/>}/>
    <Route path="/registro" element={session ? <Navigate to="/" replace/> : <LoginPage register/>}/>
    <Route path="/admin" element={<AdminOnly/>}>
      <Route index element={<DashboardPage/>}/>
      <Route path="inventario" element={<InventoryPage/>}/>
      <Route path="categorias" element={<NamedCatalog kind="categories" title="Categorías" subtitle="Organiza tu catálogo por familias."/>}/>
      <Route path="marcas" element={<NamedCatalog kind="brands" title="Marcas" subtitle="Gestiona las marcas que comercializas."/>}/>
      <Route path="proveedores" element={<ContactsPage kind="suppliers" title="Proveedores" subtitle="Tus aliados de abastecimiento."/>}/>
      <Route path="clientes" element={<ContactsPage kind="customers" title="Clientes" subtitle="Conoce y cuida tus relaciones."/>}/>
      <Route path="*" element={<PlaceholderPage title="Próximamente" description="Este módulo está preparado para crecer."/>}/>
    </Route>
    <Route path="*" element={<Navigate to="/" replace/>}/>
  </Routes>;
}
