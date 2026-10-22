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
export function useNamed(kind:"categories"|"brands"){const qc=useQueryClient();const query=useQuery({queryKey:[kind],queryFn:()=>api.named(kind)});const create=useMutation({mutationFn:api.createNamed.bind(null,kind),onSuccess:()=>qc.invalidateQueries({queryKey:[kind]})});const remove=useMutation({mutationFn:api.removeNamed.bind(null,kind),onSuccess:()=>qc.invalidateQueries({queryKey:[kind]})});return{...query,create,remove}}
export function useContacts(kind:"customers"|"suppliers"){const qc=useQueryClient();const query=useQuery({queryKey:[kind],queryFn:()=>api.contacts(kind)});const create=useMutation({mutationFn:api.createContact.bind(null,kind),onSuccess:()=>qc.invalidateQueries({queryKey:[kind]})});const remove=useMutation({mutationFn:api.removeContact.bind(null,kind),onSuccess:()=>qc.invalidateQueries({queryKey:[kind]})});return{...query,create,remove}}
