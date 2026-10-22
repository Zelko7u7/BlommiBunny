import { useContacts, useNamed } from '../hooks/useApi';
import CatalogDirectory from '../components/catalog/CatalogDirectory';

type PageProps={title:string;subtitle:string};
export function NamedCatalog({kind,...props}:PageProps&{kind:'categories'|'brands'}){
 return <NamedPage key={kind} kind={kind} {...props}/>;
}
function NamedPage({kind,...props}:PageProps&{kind:'categories'|'brands'}){
 const query=useNamed(kind);
 return <CatalogDirectory kind={kind} {...props} query={query}
  onSave={async(id,data)=>{if(id!==undefined)await query.update.mutateAsync({id,data});else await query.create.mutateAsync(data);}}
  onRemove={async id=>{await query.remove.mutateAsync(id);}}
  busy={query.create.isPending||query.update.isPending||query.remove.isPending}/>;
}
export function ContactsPage({kind,...props}:PageProps&{kind:'customers'|'suppliers'}){
 return <ContactPage key={kind} kind={kind} {...props}/>;
}
function ContactPage({kind,...props}:PageProps&{kind:'customers'|'suppliers'}){
 const query=useContacts(kind);
 return <CatalogDirectory kind={kind} {...props} query={query}
  onSave={async(id,data)=>{if(id!==undefined)await query.update.mutateAsync({id,data});else await query.create.mutateAsync(data);}}
  onRemove={async id=>{await query.remove.mutateAsync(id);}}
  busy={query.create.isPending||query.update.isPending||query.remove.isPending}/>;
}
