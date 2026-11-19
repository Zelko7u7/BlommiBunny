import { useEffect, useRef, useState, type CSSProperties, type ReactNode } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowDown, ArrowUp, Pencil, Plus, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { bannerApi, colombiaDateInput, colombiaInstant, type AdminBanner, type BannerImage, type BannerInput, type BannerTargets, type PublicBanner, type TargetType } from '../services/banners';
import BannerImageField from '../components/banners/BannerImageField';
import BannerArtwork from '../components/store/BannerArtwork';
import '../styles/banners.css';

const states = { INACTIVE: 'Inactivo', UNAVAILABLE: 'Destino no disponible', SCHEDULED: 'Programado', EXPIRED: 'Finalizado', PUBLISHED: 'Publicado' };
export default function BannersPage() {
 const qc = useQueryClient();
 const query = useQuery({ queryKey: ['admin-banners'], queryFn: bannerApi.list, refetchInterval: 15000 });
 const targets = useQuery({ queryKey: ['banner-targets'], queryFn: bannerApi.targets, refetchOnWindowFocus: true });
 const [editing, setEditing] = useState<AdminBanner | 'new' | null>(null), [deleting, setDeleting] = useState<AdminBanner | null>(null), [busy, setBusy] = useState(false), [error, setError] = useState('');
 async function refresh() { await Promise.all(['admin-banners', 'public-banners', 'banner-targets'].map(key => qc.invalidateQueries({ queryKey: [key] }))); }
 async function action(fn: () => Promise<unknown>, message: string) {
  if (busy) return; setBusy(true); setError('');
  try { await fn(); await refresh(); toast.success(message); }
  catch (e) { setError((e as Error).message || 'No pudimos guardar el cambio.'); await refresh(); throw e; }
  finally { setBusy(false); }
 }
 const banners = query.data ?? [];
 function move(index: number, delta: number) { const next = [...banners]; [next[index], next[index + delta]] = [next[index + delta], next[index]]; void action(() => bannerApi.reorder(next), 'Orden actualizado').catch(() => {}); }
 return <section className="banners-page"><header className="banners-heading"><div><h1>Banners promocionales</h1><p>Organiza las promociones que aparecen en tu tienda.</p></div><button className="primary" disabled={busy || !targets.data || !!query.isError} onClick={() => setEditing('new')}><Plus size={18}/> Nuevo banner</button></header>
  <p className="banner-help">Toda la imagen será clicable. Puedes publicar ahora, programar fechas o guardar el banner inactivo.</p>
  {(error || query.isError || targets.isError) && <div className="error" role="alert">{error || 'No pudimos cargar los banners o sus destinos.'} <button onClick={() => { void refresh(); }} disabled={busy}>Reintentar</button></div>}
  {query.isLoading && <p role="status">Cargando banners…</p>}
  {!query.isLoading && !query.isError && !banners.length && <div className="banner-empty"><h2>No hay banners</h2><p>La tienda mostrará directamente el catálogo hasta que publiques uno.</p></div>}
  <div className="banner-list">{banners.map((banner, index) => <article key={banner.id} className="banner-admin-card">
   <img src={banner.desktopImage.url.replace('/image/upload/', '/image/upload/c_limit,w_640/q_auto/f_auto/')} alt={banner.altText} loading="lazy"/>
   <div className="banner-admin-info"><h2>{banner.internalName}</h2><span className={`banner-status ${banner.publicationStatus.toLowerCase()}`}>{states[banner.publicationStatus]}</span><p>Destino: {banner.targetLabel}</p><small>Posición {index + 1}{banner.mobileImage ? ' · Con imagen para móvil' : ' · Imagen adaptable'}</small>{!banner.targetAvailable && <p className="error">Elige otro destino para volver a publicarlo.</p>}</div>
   <div className="banner-actions"><label className="banner-switch"><input type="checkbox" checked={banner.active} disabled={busy} onChange={e => { void action(() => bannerApi.status(banner, e.target.checked), e.target.checked ? 'Banner activado' : 'Banner desactivado').catch(() => {}); }}/> Activo</label><button disabled={busy || index === 0} aria-label={`Subir ${banner.internalName}`} onClick={() => move(index, -1)}><ArrowUp size={17}/></button><button disabled={busy || index === banners.length - 1} aria-label={`Bajar ${banner.internalName}`} onClick={() => move(index, 1)}><ArrowDown size={17}/></button><button disabled={busy} onClick={() => setEditing(banner)}><Pencil size={16}/> Editar</button><button disabled={busy} onClick={() => setDeleting(banner)}><Trash2 size={16}/> Eliminar</button></div>
  </article>)}</div>
  {editing && targets.data && <BannerForm key={editing === 'new' ? 'new' : editing.id} banner={editing === 'new' ? undefined : editing} targets={targets.data} initialOrder={banners.length ? Math.max(...banners.map(b => b.sortOrder)) + 1 : 0} busy={busy} onClose={() => setEditing(null)} onSave={async input => { await action(() => editing === 'new' ? bannerApi.create(input) : bannerApi.update(editing.id, input), 'Banner guardado'); setEditing(null); }}/>}
  {deleting && <BannerDialog title="Eliminar banner" busy={busy} onClose={() => setDeleting(null)}><p>¿Eliminar «{deleting.internalName}»? Desaparecerá del carrusel. Sus imágenes se conservarán en Cloudinary.</p><div className="banner-actions"><button disabled={busy} onClick={() => setDeleting(null)}>Cancelar</button><button className="primary" disabled={busy} onClick={() => { void action(() => bannerApi.remove(deleting), 'Banner eliminado').then(() => setDeleting(null)).catch(() => {}); }}>{busy ? 'Eliminando…' : 'Eliminar banner'}</button></div></BannerDialog>}
 </section>;
}
function BannerDialog({ title, busy, onClose, children }: { title: string; busy: boolean; onClose: () => void; children: ReactNode }) {
 const ref = useRef<HTMLDialogElement>(null);
 useEffect(() => { const dialog = ref.current!; dialog.showModal(); return () => dialog.close(); }, []);
 return <dialog ref={ref} className="banner-dialog" aria-labelledby="banner-dialog-title" onCancel={e => { e.preventDefault(); if (!busy) onClose(); }}><header><h2 id="banner-dialog-title">{title}</h2><button type="button" disabled={busy} onClick={onClose} aria-label="Cerrar">×</button></header>{children}</dialog>;
}
function BannerForm({ banner, targets, initialOrder, busy, onClose, onSave }: { banner?: AdminBanner; targets: BannerTargets; initialOrder: number; busy: boolean; onClose: () => void; onSave: (data: BannerInput) => Promise<void> }) {
 const [desktop, setDesktop] = useState<BannerImage | null>(banner?.desktopImage ?? null), [mobile, setMobile] = useState<BannerImage | null>(banner?.mobileImage ?? null);
 const [upload, setUpload] = useState<{ desktop: 'ready' | 'pending' | 'uploading'; mobile: 'ready' | 'pending' | 'uploading' }>({ desktop: 'ready', mobile: 'ready' });
 const [name, setName] = useState(banner?.internalName ?? ''), [alt, setAlt] = useState(banner?.altText ?? ''), [headline, setHeadline] = useState(banner?.headline ?? ''), [description, setDescription] = useState(banner?.description ?? '');
 const [target, setTarget] = useState<TargetType>(banner?.targetType ?? 'CATALOG'), [reference, setReference] = useState(String(banner?.targetReferenceId ?? '')), [section, setSection] = useState<'ABOUT' | 'FAVORITES' | ''>(banner?.section ?? '');
 const [active, setActive] = useState(banner?.active ?? false), [order, setOrder] = useState(banner?.sortOrder ?? initialOrder), [start, setStart] = useState(colombiaDateInput(banner?.startsAt)), [end, setEnd] = useState(colombiaDateInput(banner?.endsAt));
 const [requestKey] = useState(banner?.requestKey ?? crypto.randomUUID()), [error, setError] = useState(''), [previewMobile, setPreviewMobile] = useState(false), [search, setSearch] = useState('');
 const uploading = Object.values(upload).includes('uploading'), pending = Object.values(upload).some(v => v !== 'ready');
 const locked = busy || uploading;
 const options = (target === 'CATEGORY' ? targets.categories : targets.products).filter(o => o.name.toLocaleLowerCase('es').includes(search.toLocaleLowerCase('es')) || String(o.id) === reference);
 const preview: PublicBanner | null = desktop ? { id: 0, desktopImage: previewMobile && mobile ? mobile : desktop, mobileImage: null, altText: alt || 'Vista previa del banner', headline, description, href: '/#catalogo' } : null;
 async function submit(e: React.FormEvent) {
  e.preventDefault(); if (locked || pending) return;
  if (!desktop) { setError('Selecciona la imagen de escritorio.'); return; }
  try { setError(''); await onSave({ requestKey, internalName: name.trim(), desktopImage: desktop, mobileImage: mobile, altText: alt.trim(), headline, description, targetType: target, targetReferenceId: target === 'CATEGORY' || target === 'PRODUCT' ? Number(reference) : null, section: target === 'SECTION' ? section || null : null, sortOrder: order, active, startsAt: colombiaInstant(start), endsAt: colombiaInstant(end), ...(banner ? { expectedVersion: banner.version } : {}) }); }
  catch (e) { setError((e as Error).message || 'No pudimos guardar el banner.'); }
 }
 return <BannerDialog title={banner ? 'Editar banner' : 'Nuevo banner'} busy={locked} onClose={onClose}><form onSubmit={submit}><fieldset disabled={locked} className="banner-form-fields"><label>Nombre interno<input required maxLength={120} value={name} onChange={e => setName(e.target.value)} autoFocus placeholder="Ej. Promoción de labiales"/></label>
  <div className="banner-upload-grid"><BannerImageField label="Imagen de escritorio" value={desktop} disabled={busy} onChange={setDesktop} onState={state => setUpload(s => ({ ...s, desktop: state }))}/><BannerImageField label="Imagen para móvil" optional value={mobile} disabled={busy} onChange={setMobile} onState={state => setUpload(s => ({ ...s, mobile: state }))}/></div>
  <label>Descripción accesible de la imagen<input required maxLength={240} value={alt} onChange={e => setAlt(e.target.value)} placeholder="Ej. Promoción de glosses y tintas para labios"/><small>Describe la promoción para quienes usan lectores de pantalla.</small></label>
  <label>Destino al hacer clic<select value={target} onChange={e => { setTarget(e.target.value as TargetType); setReference(''); setSection(''); setSearch(''); }}><option value="CATALOG">Catálogo completo</option><option value="CATEGORY">Una categoría</option><option value="PRODUCT">Un producto</option><option value="SECTION">Una sección de la tienda</option></select></label>
  {(target === 'CATEGORY' || target === 'PRODUCT') && <><label>Buscar {target === 'CATEGORY' ? 'categoría' : 'producto'}<input type="search" value={search} onChange={e => setSearch(e.target.value)}/></label><label>{target === 'CATEGORY' ? 'Categoría' : 'Producto'}<select required value={reference} onChange={e => setReference(e.target.value)}><option value="">Selecciona una opción</option>{reference && !options.some(o => String(o.id) === reference) && <option value={reference} disabled>Destino anterior no disponible: selecciona otro</option>}{options.map(o => <option key={o.id} value={o.id}>{o.name}</option>)}</select></label></>}
  {target === 'SECTION' && <label>Sección<select required value={section} onChange={e => setSection(e.target.value as typeof section)}><option value="">Selecciona una sección</option>{targets.sections.map(o => <option key={o.id} value={o.id}>{o.name}</option>)}</select></label>}
  <div className="banner-two-columns"><label>Orden de aparición<input type="number" min={0} max={10000} required value={order} onChange={e => setOrder(Number(e.target.value))}/></label><label className="banner-switch"><input type="checkbox" checked={active} onChange={e => setActive(e.target.checked)}/> Activo en la tienda</label></div>
  <details><summary>Programar publicación (opcional)</summary><p>Hora de Colombia. Sin fechas, permanece publicado mientras esté activo.</p><div className="banner-two-columns"><label>Inicio<input type="datetime-local" value={start} onChange={e => setStart(e.target.value)}/></label><label>Finalización<input type="datetime-local" value={end} onChange={e => setEnd(e.target.value)}/></label></div></details>
  <details><summary>Texto sobre el banner (opcional)</summary><p>Déjalo vacío si tu imagen ya incluye textos. Los banners anteriores conservan aquí su título y descripción.</p><label>Título<input maxLength={120} value={headline} onChange={e => setHeadline(e.target.value)}/></label><label>Descripción<textarea maxLength={240} value={description} onChange={e => setDescription(e.target.value)}/></label></details>
  <section className="banner-preview-controls" aria-label="Vista previa antes de publicar"><h3>Vista previa</h3><div className="banner-actions"><button type="button" aria-pressed={!previewMobile} onClick={() => setPreviewMobile(false)}>Escritorio</button><button type="button" aria-pressed={previewMobile} onClick={() => setPreviewMobile(true)}>Móvil</button></div>{preview ? <div className={`banner-preview ${previewMobile ? 'mobile' : ''}`} style={{ '--preview-ratio': preview.desktopImage.width / preview.desktopImage.height } as CSSProperties}><BannerArtwork key={preview.desktopImage.url} banner={preview} eager/>{(headline || description) && <div className="banner-preview-caption">{headline && <strong>{headline}</strong>}{description && <p>{description}</p>}</div>}</div> : <p>Selecciona una imagen para ver cómo quedará.</p>}</section>
  {pending && <p role="status">Termina o descarta las subidas pendientes antes de guardar.</p>}{error && <p className="error" role="alert">{error}</p>}
  <footer className="banner-actions"><button type="button" onClick={onClose}>Cancelar</button><button className="primary" disabled={pending || !desktop}>{busy ? 'Guardando…' : active ? 'Guardar y publicar' : 'Guardar inactivo'}</button></footer>
 </fieldset></form></BannerDialog>;
}
