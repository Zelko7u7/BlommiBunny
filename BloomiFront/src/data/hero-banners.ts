export type BannerAction = { type: 'catalog' } | { type: 'category'; category: string; aliases?: readonly string[]; fallbackQuery: string };
export type HeroBanner = {
  id: string;
  image: string;
  label: string;
  title: string;
  description: string;
  buttonText: string;
  buttonAction: BannerAction;
};

// Add another entry and copy its unchanged image to public/banners; controls adapt automatically.
export const heroBanners: readonly HeroBanner[] = [
  {
    id: 'bienvenida',
    image: '/banners/banner-bienvenida.webp',
    label: 'MAQUILLAJE · CUIDADO · BELLEZA',
    title: 'Tu belleza florece aquí.',
    description: 'Descubre pequeños detalles para brillar cada día.',
    buttonText: 'Explorar colección',
    buttonAction: { type: 'catalog' },
  },
  {
    id: 'labios',
    image: '/banners/banner-labios.webp',
    label: 'LABIOS · COLOR · BRILLO',
    title: 'Un toque de brillo para ti.',
    description: 'Glosses, tintas y bálsamos para cada momento.',
    buttonText: 'Explorar productos',
    buttonAction: { type: 'category', category: 'Labios', aliases: ['Labiales'], fallbackQuery: 'labios' },
  },
];
