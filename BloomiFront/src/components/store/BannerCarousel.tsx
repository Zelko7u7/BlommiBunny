import { useEffect, useId, useRef, useState, type CSSProperties, type MouseEvent, type PointerEvent } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import type { PublicBanner } from '../../services/banners';
import BannerArtwork from './BannerArtwork';
import '../../styles/store-hero.css';

type Props = {
  banners: readonly PublicBanner[];
  getButtonHref: (banner: PublicBanner) => string;
  onButtonClick?: (event: MouseEvent<HTMLAnchorElement>, href: string) => void;
  interval?: number;
};

export default function BannerCarousel({ banners, getButtonHref, onButtonClick, interval = 6000 }: Props) {
  const [index, setIndex] = useState(0);
  const [focused, setFocused] = useState(false);
  const [touching, setTouching] = useState(false);
  const [interaction, setInteraction] = useState(0);
  const [hovered, setHovered] = useState(false);
  const [visible, setVisible] = useState(() => document.visibilityState !== 'hidden');
  const [reducedMotion, setReducedMotion] = useState(() => window.matchMedia('(prefers-reduced-motion: reduce)').matches);
  const touch = useRef<{ id: number; x: number; y: number } | null>(null);
  const suppressClick = useRef(false);
  const previous = useRef(0);
  const [failed, setFailed] = useState<string[]>([]);
  banners = banners.filter(b => !failed.includes(b.desktopImage.url));
  const id = useId();
  const active = banners.length ? index % banners.length : 0;
  const playing = banners.length > 1 && !focused && !touching && !hovered && visible && !reducedMotion;
  useEffect(() => { previous.current = active; }, [active]);

  useEffect(() => {
    const sync = () => setVisible(document.visibilityState !== 'hidden');
    document.addEventListener('visibilitychange', sync);
    return () => document.removeEventListener('visibilitychange', sync);
  }, []);
  useEffect(() => {
    const media = window.matchMedia('(prefers-reduced-motion: reduce)');
    const sync = () => setReducedMotion(media.matches);
    sync(); media.addEventListener('change', sync);
    return () => media.removeEventListener('change', sync);
  }, []);
  useEffect(() => {
    if (!playing) return;
    const timer = window.setTimeout(() => setIndex(current => (current + 1) % banners.length), interval);
    return () => window.clearTimeout(timer);
  }, [playing, active, banners.length, interval, interaction]);

  function select(next: number) {
    setInteraction(current => current + 1);
    setIndex((next + banners.length) % banners.length);
  }
  function beginTouch(event: PointerEvent<HTMLElement>) {
    if (event.pointerType !== 'touch' || !event.isPrimary || (event.target as Element).closest('button')) return;
    suppressClick.current = false;
    touch.current = { id: event.pointerId, x: event.clientX, y: event.clientY };
    setTouching(true);
  }
  function endTouch(event: PointerEvent<HTMLElement>) {
    const start = touch.current; touch.current = null; setTouching(false);
    if (!start || start.id !== event.pointerId) return;
    const dx = event.clientX - start.x, dy = event.clientY - start.y;
    if (Math.abs(dx) >= 40 && Math.abs(dx) > Math.abs(dy) * 1.3 && banners.length > 1) { suppressClick.current = true; select(active + (dx < 0 ? 1 : -1)); }
  }

  if (!banners.length) return null;
  const mobileRatio = Math.min(...banners.map(b => (b.mobileImage ?? b.desktopImage).width / (b.mobileImage ?? b.desktopImage).height));
  return <section className={`store-hero welcome-hero hero-carousel${banners.some(b => b.headline || b.description) ? ' has-captions' : ''}`} style={{ '--mobile-ratio': mobileRatio } as CSSProperties} role="region" aria-roledescription="carrusel" aria-label="Promociones de Bloomi Bunny"
    onPointerEnter={event => { if (event.pointerType !== 'touch') setHovered(true); }} onPointerLeave={() => setHovered(false)}
    onFocusCapture={() => setFocused(true)} onBlurCapture={event => { if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setFocused(false); }}
    onPointerDownCapture={() => setInteraction(current => current + 1)}
    onPointerDown={beginTouch} onPointerUp={endTouch} onPointerCancel={() => { touch.current = null; setTouching(false); }} onLostPointerCapture={() => { touch.current = null; setTouching(false); }}
    onKeyDown={event => {
      if (banners.length < 2 || !(event.target as Element).closest('.hero-controls')) return;
      if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') { event.preventDefault(); select(active + (event.key === 'ArrowRight' ? 1 : -1)); }
      else if (event.key === 'Home' || event.key === 'End') { event.preventDefault(); select(event.key === 'Home' ? 0 : banners.length - 1); }
    }}>
    <div className="hero-slides" id={`${id}-slides`}>
      {banners.map((banner, position) => <article key={banner.id} id={`${id}-${banner.id}`} className={`hero-slide${position === active ? ' is-active' : ''}`}
        aria-roledescription="diapositiva" aria-label={`${position + 1} de ${banners.length}: ${banner.altText}`} aria-hidden={position !== active} inert={position !== active}>
        <a className="hero-banner-link" href={getButtonHref(banner)} aria-label={banner.altText} draggable={false} onClick={event => {
          if (suppressClick.current && event.detail > 0) { event.preventDefault(); suppressClick.current = false; return; }
          suppressClick.current = false;
          setInteraction(current => current + 1); onButtonClick?.(event, getButtonHref(banner));
        }}>
          {(position === active || position === previous.current) && <BannerArtwork banner={banner} eager={position === active} onFailure={() => setFailed(current => [...current, banner.desktopImage.url])}/>}
          <div className="hero-caption">{banner.headline && <h2>{banner.headline}</h2>}{banner.description && <p>{banner.description}</p>}</div>
        </a>
      </article>)}
    </div>
    {banners.length > 1 && <div className="hero-controls" role="group" aria-label="Controles del carrusel">
      <button type="button" className="hero-control hero-arrow-previous" aria-label="Banner anterior" aria-controls={`${id}-slides`} onClick={() => select(active - 1)}><ChevronLeft size={18}/></button>
      <div className="hero-indicators" role="group" aria-label="Elegir banner">{banners.map((banner, position) => <button type="button" key={banner.id} className="hero-indicator" aria-label={`Mostrar banner ${position + 1}: ${banner.altText}`} aria-current={position === active ? 'true' : undefined} aria-controls={`${id}-${banner.id}`} onClick={() => select(position)}><span/></button>)}</div>
      <button type="button" className="hero-control hero-arrow-next" aria-label="Banner siguiente" aria-controls={`${id}-slides`} onClick={() => select(active + 1)}><ChevronRight size={18}/></button>
    </div>}
    <p className="hero-status" aria-live={playing ? 'off' : 'polite'} aria-atomic="true">Banner {active + 1} de {banners.length}: {banners[active].altText}</p>
  </section>;
}
