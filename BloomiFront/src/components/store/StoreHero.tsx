import { useEffect, useState, type MouseEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useLocation, useNavigate } from 'react-router-dom';
import BannerCarousel from './BannerCarousel';
import { bannerApi } from '../../services/banners';
import { heroBannerHref } from '../../services/heroNavigation';

export default function StoreHero() {
  const location = useLocation(), navigate = useNavigate();
  const query = useQuery({ queryKey: ['public-banners'], queryFn: bannerApi.public, refetchInterval: 15000, refetchOnWindowFocus: true, retry: 1 });
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    const current = Date.now();
    const end = Math.min(...(query.data ?? []).map(b => b.endsAt ? new Date(b.endsAt).getTime() : Infinity).filter(n => n > current));
    if (!Number.isFinite(end)) return;
    const timer = window.setTimeout(() => setNow(Date.now()), Math.min(end - current + 10, 2147483647));
    return () => clearTimeout(timer);
  }, [query.data, now]);
  function follow(event: MouseEvent<HTMLAnchorElement>, href: string) {
    if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    navigate(href, { preventScrollReset: href.includes('#catalogo') });
    requestAnimationFrame(() => { if (href.includes('#catalogo')) document.getElementById('catalogo')?.scrollIntoView(); else window.scrollTo(0, 0); });
  }
  if (query.isError || !Array.isArray(query.data)) return null;
  const banners = query.data.filter(b => !b.endsAt || new Date(b.endsAt).getTime() > now);
  return <BannerCarousel banners={banners} getButtonHref={banner => heroBannerHref(banner.href, location.search)} onButtonClick={follow}/>;
}
