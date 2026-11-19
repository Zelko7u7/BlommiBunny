import { http } from './http';
export type BannerImage = { url: string; publicId: string; width: number; height: number };
export type TargetType = 'CATALOG' | 'CATEGORY' | 'PRODUCT' | 'SECTION';
export type PublicBanner = { id: number; desktopImage: BannerImage; mobileImage: BannerImage | null; altText: string; headline?: string | null; description?: string | null; href: string; endsAt?: string | null };
export type BannerInput = { requestKey: string; internalName: string; desktopImage: BannerImage; mobileImage: BannerImage | null; altText: string; headline: string; description: string; targetType: TargetType; targetReferenceId: number | null; section: 'ABOUT' | 'FAVORITES' | null; sortOrder: number; active: boolean; startsAt: string | null; endsAt: string | null; expectedVersion?: number };
export type AdminBanner = BannerInput & { id: number; version: number; targetLabel: string; targetAvailable: boolean; publicationStatus: 'INACTIVE' | 'UNAVAILABLE' | 'SCHEDULED' | 'EXPIRED' | 'PUBLISHED'; href: string };
export type BannerTargets = { categories: { id: number; name: string }[]; products: { id: number; name: string }[]; sections: { id: 'ABOUT' | 'FAVORITES'; name: string }[] };
export const bannerApi = {
 public: () => http.get<PublicBanner[]>('/banners').then(r => r.data),
 list: () => http.get<AdminBanner[]>('/admin/banners').then(r => r.data),
 targets: () => http.get<BannerTargets>('/admin/banners/targets').then(r => r.data),
 create: (data: BannerInput) => http.post<AdminBanner>('/admin/banners', data).then(r => r.data),
 update: (id: number, data: BannerInput) => http.put<AdminBanner>(`/admin/banners/${id}`, data).then(r => r.data),
 status: (banner: AdminBanner, active: boolean) => http.patch(`/admin/banners/${banner.id}/status`, { active, expectedVersion: banner.version }),
 remove: (banner: AdminBanner) => http.delete(`/admin/banners/${banner.id}?expectedVersion=${banner.version}`),
 reorder: (banners: AdminBanner[]) => http.patch('/admin/banners/reorder', { banners: banners.map(b => ({ id: b.id, expectedVersion: b.version })) }),
 upload: (file: File, signal: AbortSignal, progress: (percent: number) => void) => {
  const form = new FormData(); form.append('file', file);
  return http.post<BannerImage>('/admin/banners/images', form, { headers: { 'Content-Type': undefined }, timeout: 30000, signal, onUploadProgress: e => progress(e.total ? Math.min(99, Math.round(e.loaded * 100 / e.total)) : 0) }).then(r => r.data);
 },
};
export { bannerImageUrl, bannerSrcSet, colombiaDateInput, colombiaInstant } from './bannerMedia';
