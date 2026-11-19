// Only routes returned by our banner API are accepted. No external redirects.
export function heroBannerHref(href: string, search = ''): string {
 if (href === '/nosotros' || href === '/favoritos' || /^\/producto\/[1-9][0-9]*$/.test(href)) return href;
 if (href !== '/#catalogo' && !/^\/\?categoryId=[1-9][0-9]*#catalogo$/.test(href)) return '/#catalogo';
 const destination = new URL(href, 'https://bloomibunny.invalid');
 const sort = new URLSearchParams(search).get('sort');
 if (sort === 'name' || sort === 'price-asc' || sort === 'price-desc') destination.searchParams.set('sort', sort);
 return `${destination.pathname}${destination.search}${destination.hash}`;
}
