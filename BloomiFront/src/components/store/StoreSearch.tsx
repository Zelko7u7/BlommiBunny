import { useEffect, useId, useRef, useState } from 'react';
import { Search, X } from 'lucide-react';

export default function StoreSearch({ query, onChange, onSearch }: {
  query: string;
  onChange?: (value: string) => void;
  onSearch: (value: string) => void;
}) {
  const id = useId();
  const input = useRef<HTMLInputElement>(null);
  const [value, setValue] = useState(query);
  useEffect(() => setValue(query), [query]);
  function change(next: string) { setValue(next); onChange?.(next); }
  return <form className="store-search" role="search" aria-label="Buscar productos" onSubmit={event => {
    event.preventDefault(); onSearch(value.trim());
  }}>
    <label className="store-search-label" htmlFor={`${id}-search`}>Buscar productos</label>
    <input ref={input} id={`${id}-search`} type="search" name="q" value={value} maxLength={160}
      placeholder="Buscar productos, marcas y tonos…" enterKeyHint="search" autoComplete="off"
      onChange={event => change(event.target.value)}/>
    {value && <button type="button" className="store-search-clear" aria-label="Borrar búsqueda" onClick={() => {
      change(''); input.current?.focus();
    }}><X size={18} aria-hidden="true"/></button>}
    <button type="submit" className="store-search-submit" aria-label="Buscar"><Search size={21} aria-hidden="true"/></button>
  </form>;
}
