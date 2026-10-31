import type { MouseEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import BannerCarousel from './BannerCarousel';
import { heroBanners } from '../../data/hero-banners';
import { heroButtonHref } from '../../services/heroNavigation';

export default function StoreHero({ categories = [] }: { categories?: readonly string[] }) {
  const location = useLocation(), navigate = useNavigate();
  function follow(event: MouseEvent<HTMLAnchorElement>, href: string) {
    if (href === '#catalogo' || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    navigate(href, { preventScrollReset: true });
    document.getElementById('catalogo')?.scrollIntoView();
  }
  return <BannerCarousel banners={heroBanners} getButtonHref={banner => heroButtonHref(banner.buttonAction, categories, location.search)} onButtonClick={follow}/>;
}
