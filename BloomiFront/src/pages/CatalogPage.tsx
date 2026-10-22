import { Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { toast } from 'sonner';
import { useContacts, useNamed } from '../hooks/useApi';
import { Card, Empty, Loading, PageHeader } from '../components/common/Ui';

export function NamedCatalog({ kind, title, subtitle }: { kind: 'categories' | 'brands'; title: string; subtitle: string }) {
  const [value, setValue] = useState('');
  const { data = [], isLoading, create, remove } = useNamed(kind);
  async function add(e: React.FormEvent) { e.preventDefault(); if (!value.trim()) return; await create.mutateAsync({ name: value }); setValue(''); toast.success('Registro creado'); }
  return <><PageHeader eyebrow="CATÁLOGOS" title={title} subtitle={subtitle}/><div className="catalog-grid"><Card><h3>Nuevo registro</h3><form className="stack" onSubmit={add}><label>Nombre<input value={value} onChange={e => setValue(e.target.value)} required/></label><button className="primary"><Plus/> Agregar</button></form></Card><Card>{isLoading ? <Loading/> : data.length === 0 ? <Empty/> : <div className="simple-list">{data.map(x => <div key={x.id}><div className="list-avatar">{x.name[0]}</div><div><strong>{x.name}</strong><span>{x.description || 'Activa'}</span></div><button className="icon-button danger-text" onClick={() => confirm('¿Eliminar este registro?') && remove.mutate(x.id)}><Trash2/></button></div>)}</div>}</Card></div></>;
}

export function ContactsPage({ kind, title, subtitle }: { kind: 'customers' | 'suppliers'; title: string; subtitle: string }) {
  const [name, setName] = useState(''), [phone, setPhone] = useState(''), [email, setEmail] = useState('');
  const { data = [], isLoading, create, remove } = useContacts(kind);
  async function add(e: React.FormEvent) { e.preventDefault(); await create.mutateAsync({ name, phone, email }); setName(''); setPhone(''); setEmail(''); toast.success('Registro creado'); }
  return <><PageHeader eyebrow="RELACIONES" title={title} subtitle={subtitle}/><div className="catalog-grid"><Card><h3>Nuevo registro</h3><form className="stack" onSubmit={add}><label>Nombre<input value={name} onChange={e => setName(e.target.value)} required/></label><label>Teléfono<input value={phone} onChange={e => setPhone(e.target.value)}/></label><label>Correo<input type="email" value={email} onChange={e => setEmail(e.target.value)}/></label><button className="primary"><Plus/> Guardar</button></form></Card><Card>{isLoading ? <Loading/> : data.length === 0 ? <Empty/> : <div className="simple-list">{data.map(x => <div key={x.id}><div className="list-avatar">{x.name[0]}</div><div><strong>{x.name}</strong><span>{x.email || x.phone || 'Sin datos de contacto'}</span></div><button className="icon-button danger-text" onClick={() => confirm('¿Eliminar este registro?') && remove.mutate(x.id)}><Trash2/></button></div>)}</div>}</Card></div></>;
}
