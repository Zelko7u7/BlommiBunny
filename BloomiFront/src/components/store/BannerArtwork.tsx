import { useState } from 'react';
import { bannerImageUrl, bannerSrcSet, type PublicBanner } from '../../services/banners';
export default function BannerArtwork({ banner, eager = false, onFailure }: { banner: Pick<PublicBanner, 'desktopImage' | 'mobileImage' | 'altText'>; eager?: boolean; onFailure?: () => void }) {
 const [original, setOriginal] = useState(false);
 const { desktopImage: desktop, mobileImage: mobile, altText } = banner;
 return <picture className="banner-artwork">
  {mobile && <source media="(max-width: 767px)" srcSet={original ? mobile.url : bannerSrcSet(mobile)} sizes="100vw" width={mobile.width} height={mobile.height}/>}
  <img src={original ? desktop.url : bannerImageUrl(desktop, 1800)} srcSet={original ? undefined : bannerSrcSet(desktop)} sizes="100vw" width={desktop.width} height={desktop.height} alt={altText} loading={eager ? 'eager' : 'lazy'} decoding="async" fetchPriority={eager ? 'high' : 'auto'} onError={() => { if (!original) setOriginal(true); else onFailure?.(); }}/>
 </picture>;
}
