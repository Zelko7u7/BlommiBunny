import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { recommendProducts } from '../../services/recommendations';
import type { CartItem } from '../../services/cart';
import type { StoreProduct } from '../../types';
import ProductCard from './ProductCard';
import '../../styles/recommendations.css';

type Props = { current: StoreProduct; products: StoreProduct[]; cart: CartItem[]; backTo: string; onAdd: (product: StoreProduct) => void };

export default function ProductRecommendations({ current, products, cart, backTo, onAdd }: Props) {
  const entries = useMemo(() => recommendProducts(current, products), [current, products]);
  const track = useRef<HTMLDivElement>(null);
  const id = useId();
  const [navigation, setNavigation] = useState({ overflow: false, previous: false, next: false });
  const entryKey = entries.map(entry => entry.key).join('|');
  useEffect(() => {
    const element = track.current;
    if (!element) return;
    element.scrollTo({ left: 0, behavior: 'auto' });
    let frame = 0;
    const measure = () => {
      const maximum = element.scrollWidth - element.clientWidth;
      const next = { overflow: maximum > 2, previous: element.scrollLeft > 2, next: element.scrollLeft < maximum - 2 };
      setNavigation(previous => previous.overflow === next.overflow && previous.previous === next.previous && previous.next === next.next ? previous : next);
    };
    const schedule = () => { cancelAnimationFrame(frame); frame = requestAnimationFrame(measure); };
    const observer = new ResizeObserver(schedule);
    observer.observe(element);
    if (element.firstElementChild) observer.observe(element.firstElementChild);
    element.addEventListener('scroll', schedule, { passive: true });
    measure();
    return () => { observer.disconnect(); element.removeEventListener('scroll', schedule); cancelAnimationFrame(frame); };
  }, [current.id, entryKey]);

  if (!entries.length) return null;
  const move = (direction: number) => {
    const element = track.current;
    const card = element?.firstElementChild as HTMLElement | null;
    if (!element || !card) return;
    const step = card.getBoundingClientRect().width + (parseFloat(getComputedStyle(element).columnGap) || 0);
    const cards = Math.max(1, Math.floor((element.clientWidth + 1) / step));
    element.scrollBy({ left: direction * step * cards, behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' });
  };
  return <section className="related-products" aria-labelledby={`${id}-title`}>
    <div className="related-heading"><h2 id={`${id}-title`}>TE PUEDE INTERESAR</h2>{navigation.overflow && <div className="related-navigation" role="group" aria-label="Navegar productos relacionados">
      <button type="button" disabled={!navigation.previous} onClick={() => move(-1)} aria-label="Productos relacionados anteriores" aria-controls={`${id}-track`}><ChevronLeft size={22}/></button>
      <button type="button" disabled={!navigation.next} onClick={() => move(1)} aria-label="Productos relacionados siguientes" aria-controls={`${id}-track`}><ChevronRight size={22}/></button>
    </div>}</div>
    <div ref={track} id={`${id}-track`} className="related-track" tabIndex={0} role="region" aria-roledescription="carrusel" aria-label="Productos que te pueden interesar" onKeyDown={event => {
      if (event.target !== event.currentTarget || !['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
      event.preventDefault();
      if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') move(event.key === 'ArrowLeft' ? -1 : 1);
      else track.current?.scrollTo({ left: event.key === 'Home' ? 0 : track.current.scrollWidth, behavior: 'auto' });
    }}>
      {entries.map(entry => <ProductCard key={entry.key} entry={entry} backTo={backTo} cartQuantity={cart.find(item => item.productId === entry.product!.id)?.quantity ?? 0} onAdd={onAdd}/>)}
    </div>
  </section>;
}
