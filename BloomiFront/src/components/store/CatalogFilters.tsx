import { useId } from 'react';
import { Search, X } from 'lucide-react';
import type { CatalogSort } from '../../services/catalog';

export default function CatalogFilters({ query, category, sort, categories, onChange, onReset }: {
  query: string; category: string; sort: CatalogSort; categories: string[];
  onChange: (key: 'q' | 'category' | 'sort', value: string) => void; onReset: () => void;
}) {
  const id = useId();
  return <form className="catalog-filters" role="search" aria-label="Buscar y filtrar productos" onSubmit={event => event.preventDefault()}>
    <div className="catalog-search-field">
      <label htmlFor={`${id}-search`}>Buscar productos</label>
      <div className="catalog-search-input"><Search size={18} aria-hidden="true"/>
        <input id={`${id}-search`} type="search" name="q" value={query} maxLength={160} placeholder="Nombre, marca o tonalidad…" onChange={event => onChange('q', event.target.value)}/>
        {query && <button type="button" onClick={() => { onChange('q', ''); document.getElementById(`${id}-search`)?.focus(); }} aria-label="Borrar búsqueda"><X size={17}/></button>}
      </div>
    </div>
    <div><label htmlFor={`${id}-category`}>Categoría</label><select id={`${id}-category`} name="category" value={category} onChange={event => onChange('category', event.target.value)}>
      <option value="">Todas las categorías</option>
      {category && !categories.includes(category) && <option value={category}>{category}</option>}
      {categories.map(value => <option key={value} value={value}>{value}</option>)}
    </select></div>
    <div><label htmlFor={`${id}-sort`}>Ordenar por</label><select id={`${id}-sort`} name="sort" value={sort} onChange={event => onChange('sort', event.target.value)}>
      <option value="name">Nombre: A a Z</option><option value="price-asc">Precio: menor a mayor</option><option value="price-desc">Precio: mayor a menor</option>
    </select></div>
    {(query || category || sort !== 'name') && <button type="button" className="catalog-reset" onClick={onReset}>Restablecer filtros</button>}
  </form>;
}
