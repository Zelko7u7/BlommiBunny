import { useEffect, useRef, useState } from 'react';
import { bannerApi, type BannerImage } from '../../services/banners';
import { validateImageFile } from '../../services/imageUploads';
export default function BannerImageField({ label, value, optional = false, disabled, onChange, onState }: { label: string; value: BannerImage | null; optional?: boolean; disabled: boolean; onChange: (image: BannerImage | null) => void; onState: (state: 'ready' | 'pending' | 'uploading') => void }) {
 const [file, setFile] = useState<File | null>(null), [preview, setPreview] = useState(''), [error, setError] = useState(''), [progress, setProgress] = useState(0), [busy, setBusy] = useState(false);
 const controller = useRef<AbortController | null>(null), mounted = useRef(true);
 useEffect(() => { mounted.current = true; return () => { mounted.current = false; controller.current?.abort(); }; }, []);
 useEffect(() => { if (!file) { setPreview(''); return; } const url = URL.createObjectURL(file); setPreview(url); return () => URL.revokeObjectURL(url); }, [file]);
 async function upload(selected: File) {
  const invalid = validateImageFile(selected); if (invalid) { setError(invalid); return; }
  setFile(selected); setBusy(true); setError(''); setProgress(0); onState('uploading');
  const abort = new AbortController(); controller.current = abort;
  try {
   const image = new Image(); const url = URL.createObjectURL(selected);
   try { image.src = url; await image.decode(); if (image.naturalWidth > 10000 || image.naturalHeight > 10000 || image.naturalWidth * image.naturalHeight > 20_000_000) throw new Error('Usa una imagen de hasta 20 megapíxeles y 10000 píxeles por lado.'); } finally { URL.revokeObjectURL(url); }
   if (!mounted.current) return;
   const result = await bannerApi.upload(selected, abort.signal, percent => { if (mounted.current) setProgress(percent); });
   if (mounted.current) { onChange(result); setFile(null); onState('ready'); }
  } catch (e) { if (mounted.current) { setError((e as Error).message || 'No pudimos subir la imagen. Reintenta.'); onState('pending'); } }
  finally { if (mounted.current) setBusy(false); }
 }
 return <div className="banner-image-field"><strong>{label}{optional ? ' (opcional)' : ''}</strong>
  {(preview || value) && <img src={preview || value!.url} alt={`Vista previa: ${label}`}/>}
  <label className="banner-file-button">{value ? 'Reemplazar imagen' : 'Seleccionar imagen'}<input type="file" accept="image/jpeg,image/png,image/webp" disabled={disabled || busy} onChange={e => { const next = e.target.files?.[0]; e.target.value = ''; if (next) void upload(next); }}/></label>
  <small>JPG, PNG o WebP · hasta 5 MB. {optional ? 'Si no cargas otra, se mostrará completa la de escritorio.' : 'Referencia: 1800 × 310 px.'}</small>
  {busy && <div role="status"><progress value={progress} max={100}/><span>{progress >= 99 ? 'Procesando imagen en Cloudinary…' : `Subiendo… ${progress}%`}</span></div>}
  {error && <p className="error" role="alert">{error}</p>}
  {file && !busy && <div className="banner-actions"><button type="button" disabled={disabled} onClick={() => void upload(file)}>Reintentar subida</button><button type="button" disabled={disabled} onClick={() => { setFile(null); setError(''); onState('ready'); }}>Descartar selección</button></div>}
  {optional && value && !file && <button type="button" disabled={disabled || busy} onClick={() => onChange(null)}>Usar solo la imagen de escritorio</button>}
 </div>;
}
