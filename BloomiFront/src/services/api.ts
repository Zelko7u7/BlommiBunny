import {http} from "./http";import type{Contact,Dashboard,Named,Product,ProductInput,StoreProduct,Session}from"../types";

async function storeProducts(): Promise<StoreProduct[]> {
 const products: StoreProduct[] = [];
 let page = 0;
 let totalPages = 1;
 do {
  const { data } = await http.get<{ content: StoreProduct[]; totalPages: number }>(`/store/products?size=100&page=${page}`);
  products.push(...data.content);
  totalPages = data.totalPages;
  page++;
 } while (page < totalPages);
 return [...new Map(products.map(product => [product.id, product])).values()];
}

export const api={
 sales:(filters:import('../types').SaleFilters)=>{
  const params = new URLSearchParams({ page:String(filters.page),size:'20' });
  for(const key of ['q','from','to','channel'] as const) if(filters[key])params.set(key,filters[key]);
  if(filters.status!=='all')params.set('cancelled',String(filters.status==='cancelled'));
  return http.get<import('../types').SalePage>(`/sales?${params}`).then(r=>{
   if(!Array.isArray(r.data.content))throw {code:'API_VERSION',message:'El backend necesita reiniciarse para activar el módulo de ventas.'};
   return r.data;
  });
 },
 sale:(id:number)=>http.get<import('../types').Sale>(`/sales/${id}`).then(r=>r.data),
 createSale:(data:import('../types').CreateSale)=>http.post<import('../types').Sale>('/sales',data).then(r=>r.data),
 cancelSale:(id:number,reason:string)=>http.post<import('../types').Sale>(`/sales/${id}/cancel`,{reason}).then(r=>r.data),
 saleProducts:async()=>{
  const products:Product[]=[];let page=0,totalPages=1;
  do { const {data}=await http.get<{content:Product[];totalPages:number}>(`/products?size=100&page=${page}`);products.push(...data.content);totalPages=data.totalPages;page++; } while(page<totalPages);
  return [...new Map(products.map(product=>[product.id,product])).values()];
 },
 inventorySummary:(q="",stock="all")=>http.get<import('../types').InventorySummary>(`/products/summary?q=${encodeURIComponent(q)}&stock=${encodeURIComponent(stock)}`).then(r=>r.data),
 favorites:()=>http.get<StoreProduct[]>("/favorites").then(r=>r.data),
 addFavorite:(id:number)=>http.put(`/favorites/${id}`),
 removeFavorite:(id:number)=>http.delete(`/favorites/${id}`),
 login:(username:string,password:string)=>http.post<Session>("/auth/login",{username,password}).then(r=>r.data),
 signup:(username:string,password:string)=>http.post<Session>("/auth/signup",{username,password}).then(r=>r.data),
 storeProducts,
 storeProduct:(id:number)=>http.get<StoreProduct>(`/store/products/${id}`).then(r=>r.data),
 dashboard:()=>http.get<Dashboard>("/dashboard").then(r=>{
  if(!Array.isArray(r.data.dailySales)||!Array.isArray(r.data.monthlySales))throw {message:'El backend necesita actualizarse para mostrar el dashboard real.'};
  return r.data;
 }),
 products:(q="",stock="all")=>http.get<{content:Product[]}>(`/products?q=${encodeURIComponent(q)}&stock=${encodeURIComponent(stock)}&size=100`).then(r=>r.data.content),
 createProduct:(data:ProductInput)=>http.post<Product>("/products",data).then(r=>r.data),
 updateProduct:(id:number,data:ProductInput)=>http.put<Product>(`/products/${id}`,data).then(r=>r.data),
 setProductActive:(id:number,active:boolean)=>http.patch<Product>(`/products/${id}/status`,{active}).then(r=>r.data),
 deleteProduct:(id:number)=>http.delete(`/products/${id}`),
 named:(kind:"categories"|"brands")=>http.get<Named[]>(`/${kind}`).then(r=>r.data),
 createNamed:(kind:"categories"|"brands",data:Partial<Named>)=>http.post(`/${kind}`,data),
 removeNamed:(kind:"categories"|"brands",id:number)=>http.delete(`/${kind}/${id}`),
 contacts:(kind:"customers"|"suppliers")=>http.get<Contact[]>(`/${kind}`).then(r=>r.data),
 createContact:(kind:"customers"|"suppliers",data:Partial<Contact>)=>http.post(`/${kind}`,data),
 removeContact:(kind:"customers"|"suppliers",id:number)=>http.delete(`/${kind}/${id}`),
 expenses:()=>http.get("/expenses").then(r=>r.data),
 createExpense:(data:unknown)=>http.post("/expenses",data)
};
