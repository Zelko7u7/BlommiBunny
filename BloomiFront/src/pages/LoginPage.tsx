import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, Eye, EyeOff, LockKeyhole, Sparkles } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

export default function LoginPage({ register = false }: { register?: boolean }) {
  const { login, signup } = useAuth();
  const navigate = useNavigate();
  const [user, setUser] = useState('');
  const [password, setPassword] = useState('');
  const [show, setShow] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  async function submit(e: React.FormEvent) {
    e.preventDefault(); setLoading(true); setError('');
    try {
      const session = await (register ? signup(user, password) : login(user, password));
      navigate(session.role === 'ADMIN' ? '/admin' : '/', { replace: true });
    } catch (x: any) { setError(x?.message ?? 'No fue posible continuar. Inténtalo de nuevo.'); }
    finally { setLoading(false); }
  }
  return <div className="login">
    <section className="login-story"><div className="story-content"><span className="story-tag"><Sparkles/> Belleza que florece</span><h1>Tu belleza,<br/>tu propio ritual.</h1><p>Encuentra tus favoritos y descubre nuestra colección de maquillaje y cuidado.</p></div><div className="orb one"/><div className="orb two"/></section>
    <section className="login-form"><form onSubmit={submit}>
      <Link className="login-back" to="/">← Volver a la tienda</Link>
      <div className="login-logo"><div className="brand-mark">B</div><div><strong>Bloomi Bunny</strong><span>Belleza que florece</span></div></div>
      <div className="welcome"><span>{register ? 'TU CUENTA BLOOMI' : 'BIENVENIDA DE NUEVO'}</span><h2>{register ? 'Crea tu cuenta' : 'Hola, qué gusto verte'}</h2><p>{register ? 'Regístrate para disfrutar de la tienda.' : 'Ingresa con tu usuario y contraseña.'}</p></div>
      <label>Usuario<div className="input"><input value={user} onChange={e => setUser(e.target.value)} autoComplete="username" maxLength={80} required/></div></label>
      <label>Contraseña<div className="input"><LockKeyhole/><input type={show ? 'text' : 'password'} value={password} onChange={e => setPassword(e.target.value)} autoComplete={register ? 'new-password' : 'current-password'} minLength={register ? 8 : undefined} maxLength={100} required/><button type="button" onClick={() => setShow(!show)} aria-label={show ? 'Ocultar contraseña' : 'Mostrar contraseña'}>{show ? <EyeOff/> : <Eye/>}</button></div></label>
      {register && <small>Mínimo 8 caracteres.</small>}
      {error && <p className="error" role="alert">{error}</p>}
      <button className="primary login-button" disabled={loading}>{loading ? 'Un momento…' : register ? 'Crear cuenta' : 'Ingresar'}<ArrowRight/></button>
      <p className="account-switch">{register ? '¿Ya tienes cuenta?' : '¿Aún no tienes cuenta?'} <Link to={register ? '/login' : '/registro'}>{register ? 'Inicia sesión' : 'Regístrate'}</Link></p>
    </form></section>
  </div>;
}
