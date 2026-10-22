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
 inventorySummary:(q="")=>http.get<import('../types').InventorySummary>(`/products/summary?q=${encodeURIComponent(q)}`).then(r=>r.data),
 favorites:()=>http.get<StoreProduct[]>("/favorites").then(r=>r.data),
 addFavorite:(id:number)=>http.put(`/favorites/${id}`),
 removeFavorite:(id:number)=>http.delete(`/favorites/${id}`),
 login:(username:string,password:string)=>http.post<Session>("/auth/login",{username,password}).then(r=>r.data),
 signup:(username:string,password:string)=>http.post<Session>("/auth/signup",{username,password}).then(r=>r.data),
 storeProducts,
 storeProduct:(id:number)=>http.get<StoreProduct>(`/store/products/${id}`).then(r=>r.data),
 dashboard:()=>http.get<Dashboard>("/dashboard").then(r=>r.data),
 products:(q="")=>http.get<{content:Product[]}>(`/products?q=${encodeURIComponent(q)}&size=100`).then(r=>r.data.content),
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
