import type { BannerImage } from './banners';
export function bannerImageUrl(image: BannerImage, width: number): string {
 return image.url.replace('/image/upload/', `/image/upload/c_limit,w_${Math.min(width, image.width)}/q_auto/f_auto/`);
}
export function bannerSrcSet(image: BannerImage): string {
 return [...new Set([360, 640, 960, 1280, 1800].map(w => Math.min(w, image.width)))].map(w => `${bannerImageUrl(image, w)} ${w}w`).join(', ');
}
export function colombiaDateInput(iso?: string | null): string { return iso ? new Date(new Date(iso).getTime() - 5 * 3600000).toISOString().slice(0, 16) : ''; }
export function colombiaInstant(value: string): string | null { return value ? new Date(`${value}:00-05:00`).toISOString() : null; }
