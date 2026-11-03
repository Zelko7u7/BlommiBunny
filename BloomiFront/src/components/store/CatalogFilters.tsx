import { useId, useRef, useState } from 'react';
import { ChevronDown, SlidersHorizontal } from 'lucide-react';
import type { CatalogSort } from '../../services/catalog';

export default function CatalogFilters({ category, sort, categories, onChange, onReset }: {
  category: string; sort: CatalogSort; categories: string[];
  onChange: (key: 'q' | 'category' | 'sort', value: string) => void; onReset: () => void;
}) {
  const id = useId();
  const [expanded, setExpanded] = useState(false);
  const trigger = useRef<HTMLButtonElement>(null);
  const count = Number(!!category) + Number(sort !== 'name');
  const sortLabel = sort === 'price-asc' ? 'Menor precio' : sort === 'price-desc' ? 'Mayor precio' : '';
  return <section className="catalog-filter-control" aria-label="Filtros del catálogo" onKeyDown={event => {
    if (event.key === 'Escape' && expanded) { event.preventDefault(); setExpanded(false); trigger.current?.focus(); }
  }}>
    <div className="catalog-filter-toolbar">
      <button ref={trigger} type="button" className="catalog-filter-toggle" aria-expanded={expanded} aria-controls={`${id}-panel`} onClick={() => setExpanded(current => !current)}>
        <SlidersHorizontal size={18} aria-hidden="true"/><span>{expanded ? 'Ocultar filtros' : 'Filtros'}</span>
        {count > 0 && <b aria-label={`${count} filtros activos`}>{count}</b>}
        <ChevronDown size={17} className={expanded ? 'is-expanded' : ''} aria-hidden="true"/>
      </button>
      {count > 0 && <>
        <span className="catalog-filter-summary" role="status">{[category, sortLabel].filter(Boolean).join(' · ')}</span>
        <button type="button" className="catalog-reset" onClick={onReset}>Limpiar filtros</button>
      </>}
    </div>
    <form id={`${id}-panel`} className="catalog-filters" hidden={!expanded} aria-label="Filtrar y ordenar productos" onSubmit={event => event.preventDefault()}>
    <div><label htmlFor={`${id}-category`}>Categoría</label><select id={`${id}-category`} name="category" value={category} onChange={event => onChange('category', event.target.value)}>
      <option value="">Todas las categorías</option>
      {category && !categories.includes(category) && <option value={category}>{category}</option>}
      {categories.map(value => <option key={value} value={value}>{value}</option>)}
    </select></div>
    <div><label htmlFor={`${id}-sort`}>Ordenar por</label><select id={`${id}-sort`} name="sort" value={sort} onChange={event => onChange('sort', event.target.value)}>
      <option value="name">Nombre: A a Z</option><option value="price-asc">Precio: menor a mayor</option><option value="price-desc">Precio: mayor a menor</option>
    </select></div>
    </form>
  </section>;
}
