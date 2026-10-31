import type { BannerAction } from '../data/hero-banners';

const normalizeCategory = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLocaleLowerCase('es');

export function heroButtonHref(action: BannerAction, categories: readonly string[], search: string): string {
  if (action.type === 'catalog') return '#catalogo';
  const options = [action.category, ...(action.aliases ?? [])].map(normalizeCategory);
  const category = categories.find(value => options.includes(normalizeCategory(value)));
  const params = new URLSearchParams(search);
  // A previous search/category must not hide products from the promoted category.
  params.delete('q'); params.delete('category');
  if (category) params.set('category', category);
  else params.set('q', action.fallbackQuery);
  return `?${params}#catalogo`;
}
