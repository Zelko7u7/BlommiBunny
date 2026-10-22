package com.bloomibunny.service.impl;
import com.bloomibunny.dto.ProductDtos.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.exception.*;
import com.bloomibunny.repository.*;
import com.bloomibunny.service.ProductService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
@Service @Transactional
public class ProductServiceImpl implements ProductService {
 private final ProductRepository products;private final CategoryRepository categories;private final BrandRepository brands;private final SupplierRepository suppliers;private final InventoryMovementRepository movements;
 public ProductServiceImpl(ProductRepository p,CategoryRepository c,BrandRepository b,SupplierRepository s,InventoryMovementRepository m){products=p;categories=c;brands=b;suppliers=s;movements=m;}
 @Transactional(readOnly=true) public Page<ProductResponse> list(String q,int page,int size,String sort){var pageable=PageRequest.of(page,Math.min(size,100),Sort.by(sort==null?"name":sort));return(q==null||q.isBlank()?products.findByDeletedFalse(pageable):products.search(q,pageable)).map(this::map);}
 @Transactional(readOnly=true) public Page<ProductResponse> listByStock(String q,int page,int size,String sort,String stock){
  validateStockFilter(stock);if("all".equals(stock))return list(q,page,size,sort);
  if(page<0||size<1)throw new BusinessException("Paginación inválida.");
  return products.searchByStock(q==null?"":q.trim(),stock,PageRequest.of(page,Math.min(size,100),Sort.by(sort==null?"name":sort))).map(this::map);
 }
 @Transactional(readOnly=true) public com.bloomibunny.dto.InventorySummary summaryByStock(String q,String stock){
  validateStockFilter(stock);if("all".equals(stock))return summary(q);
  return summarize(products.searchByStock(q==null?"":q.trim(),stock,Pageable.unpaged()).getContent());
 }
 private void validateStockFilter(String stock){if(!java.util.Set.of("all","low","out").contains(stock==null?"":stock))throw new BusinessException("Filtro de existencias inválido.");}
 @Transactional(readOnly=true) public java.util.List<ProductResponse> variants(String group){return products.findByVariantGroupAndActiveTrueAndDeletedFalseOrderByShadeAsc(group).stream().map(this::map).toList();}
 @Transactional(readOnly=true) public ProductResponse get(Long id){return map(find(id));}
 @Transactional(readOnly=true)
 public com.bloomibunny.dto.InventorySummary summary(String q){
  // Read all matching inventory, including disabled products; never limit totals to a page.
  var inventory=(q==null||q.isBlank()?products.findByDeletedFalse(Pageable.unpaged()):products.search(q,Pageable.unpaged())).getContent();
  return summarize(inventory);
 }
 private com.bloomibunny.dto.InventorySummary summarize(java.util.List<Product> inventory){
  long units=0,unknownProducts=0,unknownUnits=0,provisionalProducts=0,provisionalUnits=0;
  BigDecimal saleValue=BigDecimal.ZERO,costValue=BigDecimal.ZERO;
  for(var p:inventory){
   if(p.isProvisionalData()){provisionalProducts++;provisionalUnits+=p.getStock();continue;}
   units+=p.getStock();
   var stock=BigDecimal.valueOf(p.getStock());
   saleValue=saleValue.add(p.getSalePrice().multiply(stock));
   if(p.isCostConfirmed())costValue=costValue.add(p.getPurchasePrice().multiply(stock));
   else if(p.getStock()>0){unknownProducts++;unknownUnits+=p.getStock();}
  }
  return new com.bloomibunny.dto.InventorySummary(inventory.size(),units,saleValue,costValue,unknownProducts,unknownUnits,provisionalProducts,provisionalUnits);
 }
 public ProductResponse create(ProductRequest r){if(products.existsByCode(r.code().trim()))throw new BusinessException("Ya existe un producto con ese código. Usa un código diferente.");var p=new Product();apply(p,r);var saved=products.save(p);movement(saved,MovementType.ADJUSTMENT_IN,saved.getStock(),"Inventario inicial");return map(saved);}
 public ProductResponse update(Long id,ProductRequest r){var p=find(id);if(r.expectedVersion()!=null&&r.expectedVersion()!=p.getVersion())throw new org.springframework.orm.ObjectOptimisticLockingFailureException(Product.class,id);if(products.existsByCodeAndIdNot(r.code().trim(),id))throw new BusinessException("Ya existe otro producto con ese código. Usa un código diferente.");int old=p.getStock();apply(p,r);var saved=products.saveAndFlush(p);int delta=saved.getStock()-old;if(delta!=0)movement(saved,delta>0?MovementType.ADJUSTMENT_IN:MovementType.ADJUSTMENT_OUT,Math.abs(delta),"Actualización de producto");return map(saved);}
 public ProductResponse adjust(Long id,StockAdjustment r){var p=find(id);int value=p.getStock()+r.quantity();if(value<0)throw new BusinessException("El ajuste dejaría el stock en negativo");p.setStock(value);products.save(p);movement(p,r.quantity()>=0?MovementType.ADJUSTMENT_IN:MovementType.ADJUSTMENT_OUT,Math.abs(r.quantity()),r.reason());return map(p);}
 public ProductResponse setActive(Long id,boolean active){var p=find(id);p.setActive(active);var saved=products.saveAndFlush(p);return map(saved);}
 public void delete(Long id){var p=find(id);p.setDeleted(true);p.setActive(false);products.save(p);}
 private Product find(Long id){return products.findById(id).filter(p->!p.isDeleted()).orElseThrow(()->new ResourceNotFoundException("Producto "+id+" no encontrado"));}
 private void apply(Product p,ProductRequest r){p.setName(r.name().trim());p.setCode(r.code().trim());p.setCategory(categories.findById(r.categoryId()).orElseThrow(()->new ResourceNotFoundException("Categoría no encontrada")));p.setBrand(r.brandId()==null?null:brands.findById(r.brandId()).orElseThrow(()->new ResourceNotFoundException("Marca no encontrada")));p.setSupplier(r.supplierId()==null?null:suppliers.findById(r.supplierId()).orElseThrow(()->new ResourceNotFoundException("Proveedor no encontrado")));p.setDescription(r.description());p.setShade(r.shade());p.setVariantGroup(r.variantGroup()==null||r.variantGroup().isBlank()?null:r.variantGroup().trim());p.setPresentation(r.presentation());p.setPurchasePrice(r.purchasePrice());p.setSalePrice(r.salePrice());p.setStock(r.stock());p.setMinimumStock(r.minimumStock());p.setEntryDate(r.entryDate());p.setActive(r.active()==null||r.active());p.setImageUrl(r.imageUrl());if(r.provisionalData()!=null)p.setProvisionalData(r.provisionalData());if(r.costConfirmed()!=null)p.setCostConfirmed(r.costConfirmed());}
 private void movement(Product p,MovementType t,int q,String ref){var m=new InventoryMovement();m.setProduct(p);m.setType(t);m.setQuantity(q);m.setResultingStock(p.getStock());m.setReference(ref);movements.save(m);}
 private ProductResponse map(Product p){BigDecimal profit=p.isCostConfirmed()&&!p.isProvisionalData()?p.getSalePrice().subtract(p.getPurchasePrice()):null;String status=p.getStock()==0?"OUT":p.getStock()<=p.getMinimumStock()?"LOW":"OK";return new ProductResponse(p.getId(),p.getName(),p.getCode(),p.getCategory().getId(),p.getCategory().getName(),p.getBrand()==null?null:p.getBrand().getId(),p.getBrand()==null?null:p.getBrand().getName(),p.getSupplier()==null?null:p.getSupplier().getId(),p.getSupplier()==null?null:p.getSupplier().getName(),p.getDescription(),p.getShade(),p.getPresentation(),p.getPurchasePrice(),p.getSalePrice(),profit,p.getStock(),p.getMinimumStock(),p.getEntryDate(),p.isActive(),p.getImageUrl(),status,p.isProvisionalData(),p.isCostConfirmed(),p.getVersion(),p.getVariantGroup());}
}
