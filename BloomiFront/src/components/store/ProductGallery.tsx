import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { ChevronLeft, ChevronRight, LoaderCircle, RotateCcw, X, ZoomIn } from 'lucide-react';
import { productGallery } from '../../services/catalog';
import type { StoreProduct } from '../../types';

export default function ProductGallery({ product }: { product: StoreProduct }) {
  const allImages = productGallery(product);
  const [selected, setSelected] = useState<string>();
  const [failed, setFailed] = useState<Set<string>>(() => new Set());
  const [loaded, setLoaded] = useState<Set<string>>(() => new Set());
  const [zoom, setZoom] = useState(false);
  const zoomTrigger = useRef<HTMLButtonElement>(null);
  const images = allImages.filter(url => !failed.has(url));
  const image = selected && images.includes(selected) ? selected : images[0];
  const index = images.indexOf(image);
  const ready = !!image && loaded.has(image);
  const name = `${product.name}${product.shade ? ` · ${product.shade}` : ''}`;
  const move = (direction: number) => setSelected(images[(index + direction + images.length) % images.length]);
  const markFailed = (url: string) => { setFailed(current => new Set(current).add(url)); setZoom(false); };
  const keyboard = (event: KeyboardEvent) => {
    if (images.length < 2) return;
    if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') { event.preventDefault(); move(event.key === 'ArrowLeft' ? -1 : 1); }
    else if (event.key === 'Home' || event.key === 'End') { event.preventDefault(); setSelected(event.key === 'Home' ? images[0] : images[images.length - 1]); }
  };
  const controls = images.length > 1 && <>
    <button className="gallery-arrow gallery-previous" type="button" aria-label="Fotografía anterior" onClick={() => move(-1)}><ChevronLeft size={22}/></button>
    <button className="gallery-arrow gallery-next" type="button" aria-label="Fotografía siguiente" onClick={() => move(1)}><ChevronRight size={22}/></button>
  </>;

  if (!image) return <div className="detail-photo-placeholder gallery-placeholder" role="status"><span aria-hidden="true">✦</span><p>{allImages.length ? 'No pudimos cargar las fotografías.' : 'Foto próximamente'}</p>{allImages.length > 0 && <button type="button" className="gallery-retry" onClick={() => { setFailed(new Set()); setLoaded(new Set()); }}><RotateCcw size={16}/> Reintentar fotografías</button>}</div>;

  return <section className="product-gallery" aria-label={`Fotografías de ${name}`} onKeyDown={keyboard}>
    <div className="gallery-stage" aria-busy={!ready}>
      <button ref={zoomTrigger} type="button" className="detail-photo gallery-photo" disabled={!ready} onClick={() => setZoom(true)} aria-label={`Ampliar foto ${index + 1} de ${images.length} de ${name}`}>
        <img key={image} src={image} alt={`${name} · fotografía ${index + 1}`} className={ready ? 'is-loaded' : ''} onLoad={() => setLoaded(current => new Set(current).add(image))} onError={() => markFailed(image)}/>
        <span><ZoomIn size={16}/> Ampliar imagen</span>
      </button>
      {!ready && <div className="gallery-loading" role="status"><LoaderCircle className="spin" size={24}/><span>Cargando fotografía…</span></div>}
      {controls}
    </div>
    <p className="gallery-counter" aria-live="polite" aria-atomic="true">Fotografía {index + 1} de {images.length}{image === allImages[0] ? ' · Principal' : ''}</p>
    {images.length > 1 && <div className="gallery-thumbnails" role="group" aria-label="Elegir fotografía">{images.map((url, position) => <button key={url} type="button" className="gallery-thumbnail" aria-pressed={image === url} aria-label={`Ver fotografía ${position + 1} de ${images.length}${url === allImages[0] ? ', principal' : ''}`} onClick={() => setSelected(url)}>
      <img src={url} alt={`${name} · miniatura ${position + 1}`} loading="lazy" onError={() => markFailed(url)}/>
    </button>)}</div>}
    {failed.size > 0 && <p className="gallery-notice" role="status">Una fotografía no está disponible. Puedes seguir consultando las demás.</p>}
    {zoom && <GalleryZoom name={name} returnFocusTo={zoomTrigger.current} onClose={() => setZoom(false)} onKeyDown={keyboard}>
      <div className="gallery-zoom-stage"><img src={image} alt={`${name} · fotografía ${index + 1} ampliada`} onError={() => markFailed(image)}/>{controls}</div>
      <p className="gallery-counter" aria-live="polite">Fotografía {index + 1} de {images.length}</p>
    </GalleryZoom>}
  </section>;
}

function GalleryZoom({ name, returnFocusTo, onClose, onKeyDown, children }: { name: string; returnFocusTo: HTMLElement | null; onClose: () => void; onKeyDown: (event: KeyboardEvent) => void; children: React.ReactNode }) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current!;
    const trigger = returnFocusTo ?? document.activeElement as HTMLElement | null;
    const overflow = document.body.style.overflow;
    dialog.showModal(); document.body.style.overflow = 'hidden';
    return () => { dialog.close(); document.body.style.overflow = overflow; if (trigger?.isConnected) trigger.focus({ preventScroll: true }); };
  }, []);
  return <dialog ref={ref} className="detail-zoom gallery-zoom" aria-label={`Fotografías ampliadas de ${name}`} onKeyDown={event => { event.stopPropagation(); onKeyDown(event); }} onCancel={event => { event.preventDefault(); onClose(); }} onClick={event => {
    if (event.target !== event.currentTarget) return;
    const rect = event.currentTarget.getBoundingClientRect();
    if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) onClose();
  }}>
    <button className="gallery-zoom-close" type="button" aria-label="Cerrar foto" onClick={onClose} autoFocus><X size={22}/></button>
    {children}
  </dialog>;
}
