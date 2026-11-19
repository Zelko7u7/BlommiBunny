import { useEffect, useRef, useState } from 'react';
import { ArrowDown, ArrowUp, ImagePlus, Star, Trash2 } from 'lucide-react';
import { api } from '../../services/api';
import { IMAGE_TYPES, MAX_PRODUCT_IMAGES, uniqueGallery, validateImageFile } from '../../services/imageUploads';
import '../../styles/product-images.css';

type PendingImage = { id: string; file: File; preview: string; error?: string };
type Props = {
  cover: string; gallery: string[]; disabled: boolean;
  onChange: (cover: string, gallery: string[]) => void;
  onBusyChange: (busy: boolean) => void;
  onPendingChange: (pending: boolean) => void;
};

export default function ProductImagesEditor({ cover, gallery, disabled, onChange, onBusyChange, onPendingChange }: Props) {
  const input = useRef<HTMLInputElement>(null);
  const mounted = useRef(true);
  const abort = useRef<AbortController | null>(null);
  const previews = useRef(new Set<string>());
  const running = useRef(false);
  const [pending, setPending] = useState<PendingImage[]>([]);
  const [progress, setProgress] = useState('');
  const [error, setError] = useState('');
  const [uploading, setUploading] = useState(false);
  const secondary = uniqueGallery(cover, gallery);
  const urls = [...(cover ? [cover] : []), ...secondary];
  const busy = disabled || uploading;
  const release = (preview: string) => { URL.revokeObjectURL(preview); previews.current.delete(preview); };
  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; abort.current?.abort(); previews.current.forEach(url => URL.revokeObjectURL(url)); previews.current.clear(); };
  }, []);
  useEffect(() => onPendingChange(pending.length > 0), [pending.length, onPendingChange]);

  async function upload(items: PendingImage[]) {
    if (running.current || !items.length) return;
    running.current = true; setUploading(true); onBusyChange(true); setError('');
    let nextCover = cover;
    let nextGallery = [...secondary];
    const controller = new AbortController(); abort.current = controller;
    try {
      for (let index = 0; index < items.length; index++) {
        if (!mounted.current) break;
        const item = items[index];
        setProgress(`Subiendo fotografía ${index + 1} de ${items.length}…`);
        try {
          // Decode locally before sending, including files with misleading extensions.
          const image = new Image(); image.src = item.preview;
          await image.decode().catch(() => { throw new Error('No pudimos leer esta fotografía. Selecciona otra imagen.'); });
          if (image.naturalWidth > 10000 || image.naturalHeight > 10000 || image.naturalWidth * image.naturalHeight > 20_000_000) throw new Error('Usa fotografías de hasta 20 megapíxeles y 10000 píxeles por lado.');
          const result = await api.uploadProductImage(item.file, controller.signal);
          if (!mounted.current) break;
          if (!nextCover) nextCover = result.imageUrl; else nextGallery = uniqueGallery(nextCover, [...nextGallery, result.imageUrl]);
          onChange(nextCover, nextGallery);
          setPending(current => current.filter(row => row.id !== item.id));
          release(item.preview);
        } catch (failure) {
          if (!mounted.current) break;
          const message = (failure as { message?: string })?.message || 'No pudimos leer o subir esta fotografía. Inténtalo de nuevo.';
          setPending(current => current.map(row => row.id === item.id ? { ...row, error: message } : row));
        }
      }
    } finally {
      running.current = false; abort.current = null;
      if (mounted.current) { setUploading(false); onBusyChange(false); setProgress(''); }
    }
  }

  function select(files: FileList | null) {
    if (!files?.length || busy) return;
    const selected = Array.from(files);
    if (selected.length + pending.length + urls.length > MAX_PRODUCT_IMAGES) { setError('Puedes tener una portada y hasta 20 fotografías secundarias. Retira alguna antes de añadir más.'); return; }
    const invalid = selected.map(file => validateImageFile(file)).find(Boolean);
    if (invalid) { setError(invalid); return; }
    const items = selected.map(file => { const preview = URL.createObjectURL(file); previews.current.add(preview); return { id: crypto.randomUUID(), file, preview }; });
    setPending(current => [...current, ...items]);
    void upload(items);
  }

  function makeCover(index: number) {
    const next = [...urls]; const [selected] = next.splice(index, 1);
    onChange(selected, next);
  }
  function remove(index: number) {
    const next = urls.filter((_, position) => position !== index);
    onChange(next[0] ?? '', next.slice(1));
  }
  function move(index: number, delta: number) {
    const next = [...secondary]; const target = index + delta;
    if (target < 0 || target >= next.length) return;
    [next[index], next[target]] = [next[target], next[index]];
    onChange(cover, next);
  }

  return <section className="product-images-editor form-wide" aria-labelledby="product-images-heading">
    <div className="product-images-heading"><div><h3 id="product-images-heading">Fotografías del producto</h3><p>JPG, PNG o WebP · máximo 5 MB por foto · una portada y hasta 20 secundarias.</p></div><button type="button" className="inventory-secondary" disabled={busy || urls.length + pending.length >= MAX_PRODUCT_IMAGES} onClick={() => input.current?.click()}><ImagePlus size={17}/> Seleccionar fotografías</button></div>
    <input ref={input} type="file" accept={IMAGE_TYPES.join(',')} multiple hidden onChange={event => { select(event.target.files); event.target.value = ''; }}/>
    {!urls.length && !pending.length && <p className="product-images-empty">Selecciona fotos de tu computador o celular. La primera será la portada.</p>}
    {!!urls.length && <div className="product-images-grid">{urls.map((url, index) => <figure key={`${index}-${url}`} className={index === 0 ? 'is-cover' : ''}>
      <ImagePreview url={url} label={index === 0 ? 'Portada del producto' : `Fotografía secundaria ${index}`}/>
      <figcaption>{index === 0 ? <span><Star size={13}/> Portada</span> : <span>Foto {index + 1}</span>}<div className="product-image-actions">
        {index > 0 && <button type="button" disabled={busy} onClick={() => makeCover(index)} aria-label={`Usar foto ${index + 1} como portada`} title="Usar como portada"><Star size={16}/></button>}
        {index > 1 && <button type="button" disabled={busy} onClick={() => move(index - 1, -1)} aria-label={`Mover foto ${index + 1} antes`}><ArrowUp size={16}/></button>}
        {index > 0 && index < urls.length - 1 && <button type="button" disabled={busy} onClick={() => move(index - 1, 1)} aria-label={`Mover foto ${index + 1} después`}><ArrowDown size={16}/></button>}
        <button type="button" disabled={busy} onClick={() => remove(index)} aria-label={`Retirar foto ${index + 1}`}><Trash2 size={16}/></button>
      </div></figcaption>
    </figure>)}</div>}
    {!!pending.length && <div className="product-images-pending">{pending.map(item => <div key={item.id}><img src={item.preview} alt="Vista previa de fotografía pendiente"/><div><strong>{item.file.name}</strong>{item.error && <p role="alert">{item.error}</p>}</div>{!uploading && <button type="button" className="inventory-secondary" disabled={disabled} onClick={() => { setPending(current => current.filter(row => row.id !== item.id)); release(item.preview); }} aria-label={`Retirar pendiente ${item.file.name}`}><Trash2 size={16}/></button>}</div>)}{!uploading && <><p>Hay fotografías pendientes. Reintenta la subida o retíralas antes de guardar el producto.</p><button type="button" className="inventory-secondary" disabled={disabled} onClick={() => void upload(pending)}>Reintentar fotografías pendientes</button></>}</div>}
    {progress && <p role="status" aria-live="polite">{progress}</p>}
    {error && <p className="error" role="alert">{error}</p>}
    {!!urls.length && <p className="product-images-hint">Guarda el producto para aplicar los cambios de portada y galería. Retirar una foto aquí no borra su archivo.</p>}
  </section>;
}

function ImagePreview({ url, label }: { url: string; label: string }) {
  const [failed, setFailed] = useState<string>();
  return failed === url ? <div className="product-image-failed" role="status">No pudimos cargar esta foto.</div> : <img src={url} alt={label} onError={() => setFailed(url)}/>;
}
