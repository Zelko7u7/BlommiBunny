export const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
export const MAX_PRODUCT_IMAGES = 21;
export const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

export function validateImageFile(file: Pick<File, 'type' | 'size'>): string | undefined {
  if (!IMAGE_TYPES.includes(file.type)) return 'Usa fotografías JPG, PNG o WebP.';
  if (file.size < 1) return 'La fotografía está vacía. Selecciona otra imagen.';
  if (file.size > MAX_IMAGE_BYTES) return 'Cada fotografía debe pesar como máximo 5 MB.';
}

export function uniqueGallery(cover: string, gallery: string[]): string[] {
  return [...new Set(gallery.map(url => url.trim()).filter(url => !!url && url !== cover.trim()))];
}
