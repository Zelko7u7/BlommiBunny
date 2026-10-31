import {http} from "./http";import type{Contact,Dashboard,Named,Product,ProductInput,StoreProduct,Session}from"../types";

async function allProducts<T extends {id:number}>(path:string): Promise<T[]> {
 const products:T[] = [];
 let page = 0;
 let totalPages = 1;
 do {
  const { data } = await http.get<{ content:T[]; totalPages:number }>(`${path}${path.includes('?')?'&':'?'}size=100&page=${page}`);
  if(!Array.isArray(data.content)||!Number.isSafeInteger(data.totalPages)||data.totalPages<0)throw {message:'El servidor devolvió un listado incompleto. Reintenta la consulta.'};
  products.push(...data.content);
  totalPages = data.totalPages;
  page++;
 } while (page < totalPages);
 return [...new Map(products.map(product => [product.id, product])).values()];
}

export const api={
 operations:(kind:'purchases'|'expenses',filters:import('../types').OperationFilters)=>{
  const params=new URLSearchParams({page:String(filters.page),size:'20'});
  for(const key of ['q','from','to'] as const)if(filters[key])params.set(key,filters[key]);
  if(filters.status!=='all')params.set('cancelled',String(filters.status==='cancelled'));
  return http.get<import('../types').OperationHistory>(`/${kind}?${params}`).then(r=>{if(!Array.isArray(r.data.content)||typeof r.data.confirmedTotal!=='number')throw {message:'Actualiza el backend para activar compras y gastos.'};return r.data;});
 },
 operation:(kind:'purchases'|'expenses',id:number)=>http.get<import('../types').Purchase|import('../types').Expense>(`/${kind}/${id}`).then(r=>r.data),
 createPurchase:(data:import('../types').CreatePurchase)=>http.post<import('../types').Purchase>('/purchases',data).then(r=>r.data),
 createExpense:(data:import('../types').CreateExpense)=>http.post<import('../types').Expense>('/expenses',data).then(r=>r.data),
 cancelOperation:(kind:'purchases'|'expenses',id:number,reason:string)=>http.post<import('../types').Purchase|import('../types').Expense>(`/${kind}/${id}/cancel`,{reason}).then(r=>r.data),
 sales:(filters:import('../types').SaleFilters)=>{
  const params = new URLSearchParams({ page:String(filters.page),size:'20' });
  for(const key of ['q','from','to','channel','paymentStatus'] as const) if(filters[key])params.set(key,filters[key]);
  if(filters.status!=='all')params.set('cancelled',String(filters.status==='cancelled'));
  return http.get<import('../types').SalePage>(`/sales?${params}`).then(r=>{
   if(!Array.isArray(r.data.content))throw {code:'API_VERSION',message:'El backend necesita reiniciarse para activar el módulo de ventas.'};
   return r.data;
  });
 },
 sale:(id:number)=>http.get<import('../types').Sale>(`/sales/${id}`).then(r=>r.data),
 createSale:(data:import('../types').CreateSale)=>http.post<import('../types').Sale>('/sales',data).then(r=>r.data),
 cancelSale:(id:number,reason:string)=>http.post<import('../types').Sale>(`/sales/${id}/cancel`,{reason}).then(r=>r.data),
 addSalePayment:(id:number,data:import('../types').CreateSalePayment)=>http.post<import('../types').Sale>(`/sales/${id}/payments`,data).then(r=>r.data),
 cancelSalePayment:(id:number,paymentId:number,reason:string)=>http.post<import('../types').Sale>(`/sales/${id}/payments/${paymentId}/cancel`,{reason}).then(r=>r.data),
 authorizeSaleCollection:(id:number,expectedVersion:number,authorized:boolean,reason:string)=>http.post<import('../types').Sale>(`/sales/${id}/collection`,{expectedVersion,authorized,reason}).then(r=>r.data),
 reviewSalePayments:(id:number,expectedVersion:number,reason:string)=>http.post<import('../types').Sale>(`/sales/${id}/payments/review`,{expectedVersion,reason}).then(r=>r.data),
 saleProducts:()=>allProducts<Product>('/products'),
 inventorySummary:(q="",stock="all")=>http.get<import('../types').InventorySummary>(`/products/summary?q=${encodeURIComponent(q)}&stock=${encodeURIComponent(stock)}`).then(r=>r.data),
 favorites:()=>http.get<StoreProduct[]>("/favorites").then(r=>r.data),
 addFavorite:(id:number)=>http.put(`/favorites/${id}`),
 removeFavorite:(id:number)=>http.delete(`/favorites/${id}`),
 login:(username:string,password:string)=>http.post<Session>("/auth/login",{username,password}).then(r=>r.data),
 signup:(username:string,password:string)=>http.post<Session>("/auth/signup",{username,password}).then(r=>r.data),
 storeProducts:()=>allProducts<StoreProduct>('/store/products'),
 storeProduct:(id:number)=>http.get<StoreProduct>(`/store/products/${id}`).then(r=>r.data),
 dashboard:()=>http.get<Dashboard>("/dashboard").then(r=>{
  if(!Array.isArray(r.data.dailySales)||!Array.isArray(r.data.monthlySales)||typeof r.data.authorizedCollectionsMonth!=='number')throw {message:'El backend necesita reiniciarse para mostrar cobros, abonos y saldos.'};
  return r.data;
 }),
 products:(q="",stock="all")=>allProducts<Product>(`/products?q=${encodeURIComponent(q)}&stock=${encodeURIComponent(stock)}`),
 createProduct:(data:ProductInput)=>http.post<Product>("/products",data).then(r=>r.data),
 updateProduct:(id:number,data:ProductInput)=>http.put<Product>(`/products/${id}`,data).then(r=>r.data),
 setProductActive:(id:number,active:boolean)=>http.patch<Product>(`/products/${id}/status`,{active}).then(r=>r.data),
 deleteProduct:(id:number)=>http.delete(`/products/${id}`),
 named:(kind:"categories"|"brands")=>http.get<Named[]>(`/${kind}`).then(r=>r.data),
 createNamed:(kind:"categories"|"brands",data:Partial<Named>)=>http.post(`/${kind}`,data),
 updateNamed:(kind:"categories"|"brands",id:number,data:Partial<Named>)=>http.put(`/${kind}/${id}`,data),
 removeNamed:(kind:"categories"|"brands",id:number)=>http.delete(`/${kind}/${id}`),
 contacts:(kind:"customers"|"suppliers")=>http.get<Contact[]>(`/${kind}`).then(r=>r.data),
 createContact:(kind:"customers"|"suppliers",data:Partial<Contact>)=>http.post(`/${kind}`,data),
 updateContact:(kind:"customers"|"suppliers",id:number,data:Partial<Contact>)=>http.put(`/${kind}/${id}`,data),
 removeContact:(kind:"customers"|"suppliers",id:number)=>http.delete(`/${kind}/${id}`)
};
