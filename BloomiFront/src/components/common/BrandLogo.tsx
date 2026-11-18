import '../../styles/brand.css';

type BrandLogoProps = {
  className?: string;
  compact?: boolean;
  loading?: 'eager' | 'lazy';
  priority?: boolean;
};

/** Official artwork, shared by the store, welcome screen and administration. */
export default function BrandLogo({ className = '', compact = false, loading = 'eager', priority = false }: BrandLogoProps) {
  const file = compact ? 'isotipoB-Transparente.svg' : 'Logotipo_BloomiBunny.svg';
  return <img
    className={`brand-logo${compact ? ' brand-logo--compact' : ''}${className ? ` ${className}` : ''}`}
    src={`${import.meta.env.BASE_URL}brand/${file}`}
    alt="Bloomi Bunny"
    width={compact ? 4020 : 8663}
    height={compact ? 4020 : 4142}
    loading={loading}
    decoding="async"
    fetchPriority={priority ? 'high' : 'auto'}
  />;
}
