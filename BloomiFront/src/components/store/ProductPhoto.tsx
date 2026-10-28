import { useState } from 'react';

export default function ProductPhoto({ src, previewSrc, name, compact = false }: { src?: string; previewSrc?: string; name: string; compact?: boolean }) {
  const [failedSource, setFailedSource] = useState<string>();
  const [hovered, setHovered] = useState(false);
  const [requestedPreview, setRequestedPreview] = useState<string>();
  const [loadedPreview, setLoadedPreview] = useState<string>();
  const [failedPreview, setFailedPreview] = useState<string>();
  if (!src || failedSource === src) return <span className={`product-photo-placeholder${compact ? ' compact' : ''}`}><span aria-hidden="true">✦</span><small>Foto próximamente</small></span>;
  if (previewSrc && previewSrc !== src) return <span className={`product-photo-preview ${hovered && loadedPreview === previewSrc && failedPreview !== previewSrc ? 'is-previewing' : ''}`} onPointerEnter={event => {
    if (event.pointerType !== 'mouse' || !matchMedia('(hover: hover) and (pointer: fine)').matches) return;
    setHovered(true);
    if (failedPreview !== previewSrc) setRequestedPreview(previewSrc);
  }} onPointerLeave={() => setHovered(false)}>
    <img className="product-photo-primary" src={src} alt={name} loading="lazy" decoding="async" onError={() => setFailedSource(src)}/>
    {requestedPreview === previewSrc && <img className="product-photo-secondary" src={previewSrc} alt="" aria-hidden="true" decoding="async" onLoad={() => setLoadedPreview(previewSrc)} onError={() => setFailedPreview(previewSrc)}/>}
  </span>;
  return <img src={src} alt={name} loading="lazy" decoding="async" onError={() => setFailedSource(src)}/>;
}
