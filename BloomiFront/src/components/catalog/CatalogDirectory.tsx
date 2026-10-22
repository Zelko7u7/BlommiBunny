import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Eye, Pencil, Plus, RefreshCw, Search, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { Card, Loading, PageHeader } from '../common/Ui';
import { InventoryDialog } from '../inventory/InventoryDialogs';
import type { Contact, Named } from '../../types';
import '../../styles/catalog-admin.css';

type Kind='categories'|'brands'|'suppliers'|'customers';
type Entry=Named&Partial<Omit<Contact,'id'|'name'|'active'>>;
type EntryInput=Omit<Entry,'id'>;
type Query={data?:Entry[];isLoading:boolean;isError:boolean;isFetching:boolean;refetch:()=>unknown};
type Props={kind:Kind;title:string;subtitle:string;query:Query;busy:boolean;onSave:(id:number|undefined,data:EntryInput)=>Promise<void>;onRemove:(id:number)=>Promise<void>};
const labels:Record<Kind,{one:string;create:string;edit:string}>={
 categories:{one:'categoría',create:'Nueva categoría',edit:'Editar categoría'},
 brands:{one:'marca',create:'Nueva marca',edit:'Editar marca'},
 suppliers:{one:'proveedor',create:'Nuevo proveedor',edit:'Editar proveedor'},
 customers:{one:'cliente',create:'Nuevo cliente',edit:'Editar cliente'},
};
const normalize=(value:string)=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('es').replace(/\s+/g,' ').trim();
const birthday=(value?:string)=>value?new Date(`${value}T12:00:00`).toLocaleDateString('es-CO',{day:'numeric',month:'long',year:'numeric'}):'Sin registrar';
function failureMessage(error:unknown,deleting=false){
 const value=error as {code?:string;message?:string;fields?:Record<string,string>};
 if(value?.code==='DATA_CONFLICT')return deleting?'Este registro está vinculado a otros datos y no se puede eliminar. Puedes editarlo y marcarlo como inactivo para conservar su historial.':'Hay un registro con ese nombre o datos en conflicto. Revisa la información e inténtalo de nuevo.';
 return value?.fields?Object.values(value.fields).join('. '):value?.message??'No pudimos guardar los cambios. Inténtalo de nuevo.';
}

export default function CatalogDirectory({kind,title,subtitle,query,busy,onSave,onRemove}:Props){
 const contact=kind==='customers'||kind==='suppliers';
 const [params,setParams]=useSearchParams();
 const search=params.get('q')??'';
 const status=['active','inactive'].includes(params.get('status')??'')?params.get('status')!:'all';
 const descending=params.get('sort')==='desc';
 const [editor,setEditor]=useState<{entry?:Entry}|null>(null);
 const [detail,setDetail]=useState<Entry|null>(null);
 const [deleting,setDeleting]=useState<Entry|null>(null);
 const [deleteError,setDeleteError]=useState('');
 const entries=query.data??[];
 const active=entries.filter(entry=>entry.active).length;
 const filtered=entries.filter(entry=>{
  if(status==='active'&&!entry.active||status==='inactive'&&entry.active)return false;
  const text=[entry.name,entry.description,entry.phone,entry.email,entry.address,entry.notes,kind==='customers'?entry.birthday:'',kind==='customers'?birthday(entry.birthday):''].filter(Boolean).join(' ');
  return normalize(text).includes(normalize(search));
 }).sort((a,b)=>(descending?-1:1)*a.name.localeCompare(b.name,'es',{sensitivity:'base',numeric:true})||a.id-b.id);
 const totalPages=Math.max(1,Math.ceil(filtered.length/12));
 const requested=Number(params.get('page')??'1');
 const page=Math.min(totalPages,Number.isSafeInteger(requested)&&requested>0?requested:1);
 const visible=filtered.slice((page-1)*12,page*12);
 const change=(key:string,value:string)=>setParams(current=>{const next=new URLSearchParams(current);next.delete('page');if(value)next.set(key,value);else next.delete(key);return next;},{replace:true});
 const pageTo=(value:number)=>setParams(current=>{const next=new URLSearchParams(current);if(value===1)next.delete('page');else next.set('page',String(value));return next;});
 const filteredView=!!search||status!=='all';
 async function save(id:number|undefined,data:EntryInput){await onSave(id,data);setEditor(null);if(id===undefined)setParams({},{replace:true});toast.success(id===undefined?'Registro creado':'Cambios guardados');}
 async function remove(){if(!deleting||busy)return;setDeleteError('');try{await onRemove(deleting.id);setDeleting(null);toast.success('Registro eliminado');}catch(error){setDeleteError(failureMessage(error,true));}}
 return <div className="catalog-admin">
  <PageHeader eyebrow={contact?'RELACIONES':'CATÁLOGOS'} title={title} subtitle={subtitle} action={<button className="primary" disabled={busy||query.isLoading||query.isError} onClick={()=>setEditor({})}><Plus size={18}/>{labels[kind].create}</button>}/>
  {query.isLoading?<Loading/>:query.isError?<Card><div className="catalog-load-error" role="alert"><h2>No pudimos actualizar {title.toLocaleLowerCase('es')}</h2><p>Revisa tu conexión y vuelve a intentarlo.</p><button className="primary" onClick={()=>query.refetch()} disabled={query.isFetching}>Reintentar</button></div></Card>:<>
   <div className="catalog-counts" aria-label="Resumen de registros"><Card><span>Total de registros</span><strong>{entries.length}</strong></Card><Card><span>Activos</span><strong>{active}</strong></Card><Card><span>Inactivos</span><strong>{entries.length-active}</strong></Card></div>
   <Card className="catalog-directory"><div className="catalog-toolbar">
    <label className="catalog-search"><span>Buscar {title.toLocaleLowerCase('es')}</span><div><Search size={18}/><input type="search" value={search} onChange={event=>change('q',event.target.value)} placeholder={contact?'Nombre, teléfono, correo, dirección o notas…':'Nombre o descripción…'}/></div></label>
    <label>Estado<select value={status} onChange={event=>change('status',event.target.value==='all'?'':event.target.value)}><option value="all">Todos</option><option value="active">Activos</option><option value="inactive">Inactivos</option></select></label>
    <label>Orden<select value={descending?'desc':'asc'} onChange={event=>change('sort',event.target.value==='asc'?'':event.target.value)}><option value="asc">Nombre: A a Z</option><option value="desc">Nombre: Z a A</option></select></label>
    <button type="button" className="catalog-secondary catalog-refresh" aria-label="Actualizar registros" disabled={query.isFetching||busy} onClick={()=>query.refetch()}><RefreshCw size={17} className={query.isFetching?'spin':''}/><span>Actualizar</span></button>
   </div>
   <div className="catalog-result-bar"><p role="status">{filtered.length===0?'0':`${(page-1)*12+1}–${Math.min(page*12,filtered.length)}`} de {filtered.length} registros{filteredView?` · ${entries.length} en total`:''}</p>{(filteredView||descending)&&<button className="catalog-clear" onClick={()=>setParams({},{replace:true})}>Limpiar filtros</button>}</div>
   {visible.length===0?<div className="catalog-empty"><Search size={28}/><h2>{entries.length===0?`Aún no hay ${title.toLocaleLowerCase('es')}`:'No encontramos coincidencias'}</h2><p>{entries.length===0?`Usa «${labels[kind].create}» para guardar el primer registro.`:'Prueba otra búsqueda o cambia el filtro de estado.'}</p></div>:<div className={contact?'catalog-contact-grid':'catalog-named-list'}>{visible.map(entry=><article key={entry.id} className="catalog-entry">
    <div className="catalog-entry-heading"><span className="catalog-avatar" aria-hidden="true">{entry.name.slice(0,1).toLocaleUpperCase('es')}</span><div><h2>{entry.name}</h2><span className={`catalog-status ${entry.active?'active':'inactive'}`}>{entry.active?'Activo':'Inactivo'}</span><small>Registro #{entry.id}</small></div></div>
    {contact?<><dl className="catalog-contact-fields"><Field label="Teléfono">{entry.phone?<a href={`tel:${entry.phone.replace(/[^+\d]/g,'')}`}>{entry.phone}</a>:undefined}</Field><Field label="Correo">{entry.email?<a href={`mailto:${entry.email}`}>{entry.email}</a>:undefined}</Field><Field label="Dirección">{entry.address}</Field>{kind==='customers'&&<Field label="Cumpleaños">{birthday(entry.birthday)}</Field>}</dl><div className="catalog-notes"><h3>Notas</h3><p className={!entry.notes?'catalog-missing':''}>{entry.notes||'Sin notas registradas'}</p></div></>:<div className="catalog-description"><h3>Descripción</h3><p className={!entry.description?'catalog-missing':''}>{entry.description||'Sin descripción registrada'}</p></div>}
    <div className="catalog-entry-actions"><button className="catalog-secondary" aria-label={`Ver detalle de ${entry.name}`} onClick={()=>setDetail(entry)}><Eye size={16}/>Ver detalle</button><button className="catalog-secondary" disabled={busy} aria-label={`Editar ${entry.name}`} onClick={()=>setEditor({entry})}><Pencil size={16}/>Editar</button><button className="catalog-delete" disabled={busy} aria-label={`Eliminar ${entry.name}`} onClick={()=>{setDeleteError('');setDeleting(entry);}}><Trash2 size={16}/><span>Eliminar</span></button></div>
   </article>)}</div>}
   {totalPages>1&&<nav className="catalog-pagination" aria-label="Páginas de registros"><button className="catalog-secondary" disabled={page===1} onClick={()=>pageTo(page-1)}>Anterior</button><span>Página {page} de {totalPages}</span><button className="catalog-secondary" disabled={page===totalPages} onClick={()=>pageTo(page+1)}>Siguiente</button></nav>}
   </Card>
  </>}
  {editor&&<EntryEditor key={editor.entry?.id??'new'} kind={kind} entry={editor.entry} busy={busy} onClose={()=>setEditor(null)} onSave={save}/>}
  {detail&&<InventoryDialog title={`Detalle de ${labels[kind].one}`} onClose={()=>setDetail(null)}><div className="catalog-detail"><h3>{detail.name}</h3><span className={`catalog-status ${detail.active?'active':'inactive'}`}>{detail.active?'Activo':'Inactivo'}</span><dl><Field label="Número de registro">#{detail.id}</Field><Field label="Nombre">{detail.name}</Field>{contact?<><Field label="Teléfono">{detail.phone}</Field><Field label="Correo">{detail.email}</Field><Field label="Dirección">{detail.address}</Field>{kind==='customers'&&<Field label="Cumpleaños">{birthday(detail.birthday)}</Field>}<Field label="Notas">{detail.notes}</Field></>:<Field label="Descripción">{detail.description}</Field>}</dl><div className="catalog-form-actions"><button className="catalog-secondary" onClick={()=>setDetail(null)}>Cerrar</button><button className="primary" disabled={busy} onClick={()=>{setDetail(null);setEditor({entry:detail});}}><Pencil size={17}/>Editar información</button></div></div></InventoryDialog>}
  {deleting&&<InventoryDialog title={`Eliminar ${labels[kind].one}`} busy={busy} onClose={()=>setDeleting(null)}><div className="catalog-confirm"><p>¿Eliminar <strong>{deleting.name}</strong>?</p><p>Esta acción es permanente. Si quieres conservar sus datos, puedes editar el registro y marcarlo como inactivo.</p>{deleteError&&<p className="error" role="alert">{deleteError}</p>}<div className="catalog-form-actions"><button className="catalog-secondary" disabled={busy} onClick={()=>setDeleting(null)}>Cancelar</button><button className="catalog-danger-button" disabled={busy} onClick={remove}>{busy?'Eliminando…':'Eliminar registro'}</button></div></div></InventoryDialog>}
 </div>;
}

function Field({label,children}:{label:string;children?:ReactNode}){return <div><dt>{label}</dt><dd>{children||<span className="catalog-missing">Sin registrar</span>}</dd></div>;}

function EntryEditor({kind,entry,busy,onClose,onSave}:{kind:Kind;entry?:Entry;busy:boolean;onClose:()=>void;onSave:(id:number|undefined,data:EntryInput)=>Promise<void>}){
 const contact=kind==='customers'||kind==='suppliers';
 const formRef=useRef<HTMLFormElement>(null);
 useEffect(()=>{formRef.current?.querySelector<HTMLInputElement>('input[name=name]')?.focus();},[]);
 const [error,setError]=useState('');
 const [active,setActive]=useState(entry?.active??true);
 async function submit(event:FormEvent<HTMLFormElement>){
  event.preventDefault();if(busy)return;
  const form=new FormData(event.currentTarget);
  const value=(name:string)=>String(form.get(name)??'').trim();
  if(!value('name')){setError('Escribe un nombre que no esté vacío.');return;}
  const payload:EntryInput={name:value('name'),active};
  if(contact){Object.assign(payload,{phone:value('phone'),email:value('email'),address:value('address'),notes:value('notes')});if(kind==='customers')payload.birthday=value('birthday')||undefined;}
  else payload.description=value('description');
  setError('');try{await onSave(entry?.id,payload);}catch(failure){setError(failureMessage(failure));}
 }
 return <InventoryDialog title={entry?labels[kind].edit:labels[kind].create} busy={busy} onClose={onClose}><form ref={formRef} className="catalog-form" onSubmit={submit}>
  <p>Los campos opcionales quedan disponibles en el listado y en el detalle del registro.</p>
  <fieldset disabled={busy}><legend className="catalog-sr-only">Información de {labels[kind].one}</legend><div className="catalog-form-grid">
   <label className="catalog-wide"><span>Nombre <b aria-hidden="true">*</b></span><input name="name" defaultValue={entry?.name??''} maxLength={contact?140:120} required autoComplete={kind==='customers'?'name':'organization'}/></label>
   {contact?<><label>Teléfono<input name="phone" type="tel" defaultValue={entry?.phone??''} maxLength={30} autoComplete="tel"/></label><label>Correo electrónico<input name="email" type="email" defaultValue={entry?.email??''} maxLength={150} autoComplete="email"/></label><label className="catalog-wide">Dirección<textarea name="address" defaultValue={entry?.address??''} maxLength={240} rows={2} autoComplete="street-address"/></label>{kind==='customers'&&<label>Cumpleaños<input name="birthday" type="date" defaultValue={entry?.birthday??''} autoComplete="bday"/></label>}<label className="catalog-wide">Notas<textarea name="notes" defaultValue={entry?.notes??''} maxLength={1000} rows={4}/><small>Hasta 1.000 caracteres.</small></label></>:<label className="catalog-wide">Descripción<textarea name="description" defaultValue={entry?.description??''} maxLength={500} rows={5}/><small>Hasta 500 caracteres.</small></label>}
   <label className="catalog-active-toggle catalog-wide"><input name="active" type="checkbox" checked={active} onChange={event=>setActive(event.target.checked)}/><span>Registro activo<small>Desmárcalo para conservar la información como inactiva.</small></span></label>
  </div></fieldset>
  {error&&<p className="error" role="alert">{error}</p>}
  <div className="catalog-form-actions"><button type="button" className="catalog-secondary" disabled={busy} onClick={onClose}>Cancelar</button><button type="submit" className="primary" disabled={busy}>{busy?'Guardando…':entry?'Guardar cambios':'Crear registro'}</button></div>
 </form></InventoryDialog>;
}
