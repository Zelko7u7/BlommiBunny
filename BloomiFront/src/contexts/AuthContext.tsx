import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { api } from '../services/api';
import type { Session } from '../types';
import { useQueryClient } from '@tanstack/react-query';

const STORAGE_KEY = 'bloomi.session';
type AuthValue = { session: Session | null; login: (username: string, password: string) => Promise<Session>; signup: (username: string, password: string) => Promise<Session>; logout: () => void };
const AuthContext = createContext<AuthValue | undefined>(undefined);

function readSession(): Session | null {
  try {
    const value = localStorage.getItem(STORAGE_KEY);
    if (!value) return null;
    const session = JSON.parse(value) as Session;
    const payload = JSON.parse(atob(session.token.split('.')[1])) as { exp?: number };
    if (payload.exp && payload.exp * 1000 <= Date.now()) { localStorage.removeItem(STORAGE_KEY); return null; }
    return session;
  } catch { localStorage.removeItem(STORAGE_KEY); return null; }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [session, setSession] = useState<Session | null>(readSession);
  const logout = () => { localStorage.removeItem(STORAGE_KEY); queryClient.clear(); setSession(null); };
  const saveSession = (next: Session) => { queryClient.clear(); localStorage.setItem(STORAGE_KEY, JSON.stringify(next)); setSession(next); return next; };
  const login = async (username: string, password: string) => saveSession(await api.login(username.trim(), password));
  const signup = async (username: string, password: string) => saveSession(await api.signup(username.trim(), password));
  useEffect(() => { const onLogout = () => logout(); window.addEventListener('bloomi:logout', onLogout); return () => window.removeEventListener('bloomi:logout', onLogout); }, []);
  return <AuthContext.Provider value={{ session, login, signup, logout }}>{children}</AuthContext.Provider>;
}
export function useAuth() { const context = useContext(AuthContext); if (!context) throw new Error('useAuth debe usarse dentro de AuthProvider'); return context; }
