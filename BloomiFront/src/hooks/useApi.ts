import{useMutation,useQuery,useQueryClient}from"@tanstack/react-query";import{api}from"../services/api";
export const useDashboard=()=>useQuery({queryKey:["dashboard"],queryFn:api.dashboard,refetchInterval:15000,refetchOnWindowFocus:true});
export const useProducts=(q="",stock="all")=>useQuery({queryKey:["products",q,stock],queryFn:()=>api.products(q,stock),refetchInterval:15000,refetchOnWindowFocus:true});
export const useInventorySummary=(q="",stock="all")=>useQuery({queryKey:["inventory-summary",q,stock],queryFn:()=>api.inventorySummary(q,stock),refetchInterval:15000,refetchOnWindowFocus:true});
export const useStoreProducts=()=>useQuery({queryKey:["store-products"],queryFn:api.storeProducts,refetchInterval:15000});
export function useFavorites(username?:string){
 const qc=useQueryClient();
 const query=useQuery({queryKey:['favorites',username],queryFn:api.favorites,enabled:!!username,refetchInterval:15000});
 const change=useMutation({mutationFn:({id,saved}:{id:number;saved:boolean})=>saved?api.removeFavorite(id):api.addFavorite(id),onSuccess:()=>qc.invalidateQueries({queryKey:['favorites',username]})});
 return {...query,change};
}
export const validProductId=(id:string|undefined)=>!!id&&/^[1-9][0-9]*$/.test(id)&&Number.isSafeInteger(Number(id));
export const useStoreProduct=(id:string|undefined)=>useQuery({queryKey:["store-product",id],queryFn:()=>api.storeProduct(Number(id)),enabled:validProductId(id),refetchInterval:15000});
export function useProductActions(){
 const qc=useQueryClient();
 const refresh=async()=>{await Promise.all([qc.invalidateQueries({queryKey:["products"]}),qc.invalidateQueries({queryKey:["sale-products"]}),qc.invalidateQueries({queryKey:["inventory-summary"]}),qc.invalidateQueries({queryKey:["store-products"]}),qc.invalidateQueries({queryKey:["store-product"]}),qc.invalidateQueries({queryKey:["favorites"]}),qc.invalidateQueries({queryKey:["dashboard"]})]);};
 const create=useMutation({mutationFn:api.createProduct,onSuccess:refresh});
 const update=useMutation({mutationFn:({id,data}:{id:number;data:import('../types').ProductInput})=>api.updateProduct(id,data),onSuccess:refresh,onError:(error:any)=>{if(error?.code==='CONFLICT'||error?.code==='NOT_FOUND')return qc.invalidateQueries({queryKey:["products"]});}});
 const setActive=useMutation({mutationFn:({id,active}:{id:number;active:boolean})=>api.setProductActive(id,active),onSuccess:refresh});
 const remove=useMutation({mutationFn:api.deleteProduct,onSuccess:refresh});
 return{create,update,setActive,remove};
}
function useCatalogRefresh(kind:string){
 const qc=useQueryClient();
 return ()=>Promise.all([kind,'products','sale-products','inventory-summary','store-products','store-product','favorites','dashboard'].map(key=>qc.invalidateQueries({queryKey:[key]})));
}
export function useNamed(kind:"categories"|"brands"){
 const refresh=useCatalogRefresh(kind);
 const query=useQuery({queryKey:[kind],queryFn:()=>api.named(kind),refetchOnWindowFocus:true});
 const create=useMutation({mutationFn:(data:Partial<import('../types').Named>)=>api.createNamed(kind,data),onSuccess:refresh});
 const update=useMutation({mutationFn:({id,data}:{id:number;data:Partial<import('../types').Named>})=>api.updateNamed(kind,id,data),onSuccess:refresh});
 const remove=useMutation({mutationFn:(id:number)=>api.removeNamed(kind,id),onSuccess:refresh});
 return {...query,create,update,remove};
}
export function useContacts(kind:"customers"|"suppliers"){
 const refresh=useCatalogRefresh(kind);
 const query=useQuery({queryKey:[kind],queryFn:()=>api.contacts(kind),refetchOnWindowFocus:true});
 const create=useMutation({mutationFn:(data:Partial<import('../types').Contact>)=>api.createContact(kind,data),onSuccess:refresh});
 const update=useMutation({mutationFn:({id,data}:{id:number;data:Partial<import('../types').Contact>})=>api.updateContact(kind,id,data),onSuccess:refresh});
 const remove=useMutation({mutationFn:(id:number)=>api.removeContact(kind,id),onSuccess:refresh});
 return {...query,create,update,remove};
}
