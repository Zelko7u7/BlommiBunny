import { useState } from 'react';

export default function ProductPhoto({ src, name, compact = false }: { src?: string; name: string; compact?: boolean }) {
  const [failedSource, setFailedSource] = useState<string>();
  if (!src || failedSource === src) return <span className={`product-photo-placeholder${compact ? ' compact' : ''}`}><span aria-hidden="true">✦</span><small>Foto próximamente</small></span>;
  return <img src={src} alt={name} loading="lazy" decoding="async" onError={() => setFailedSource(src)}/>;
}
