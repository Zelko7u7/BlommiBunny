import type{ReactNode}from"react";import{LoaderCircle,Search}from"lucide-react";
export function PageHeader({eyebrow,title,subtitle,action}:{eyebrow?:string;title:string;subtitle:string;action?:ReactNode}){return <header className="page-header"><div>{eyebrow&&<span className="eyebrow">{eyebrow}</span>}<h1>{title}</h1><p>{subtitle}</p></div>{action}</header>}
export function Card({children,className=""}:{children:ReactNode;className?:string}){return <section className={`card ${className}`}>{children}</section>}
export function Loading(){return <div className="state"><LoaderCircle className="spin"/> Cargando información…</div>}
export function Empty({text="No hay registros todavía."}:{text?:string}){return <div className="state">{text}</div>}
export function SearchBox({value,onChange,placeholder="Buscar…"}:{value:string;onChange:(v:string)=>void;placeholder?:string}){return <label className="search"><Search size={18}/><input value={value} onChange={e=>onChange(e.target.value)} placeholder={placeholder}/></label>}
export const money=(n=0)=>new Intl.NumberFormat("es-CO",{style:"currency",currency:"COP",maximumFractionDigits:0}).format(n);
