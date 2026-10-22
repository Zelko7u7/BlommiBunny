package com.bloomibunny.service;

import com.bloomibunny.dto.TransactionDtos.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.exception.*;
import com.bloomibunny.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @Transactional
public class AdminPurchaseService {
 private final PurchaseRepository purchases;private final ProductRepository products;private final SupplierRepository suppliers;private final InventoryMovementRepository movements;private final OperationSupport ops;
 public AdminPurchaseService(PurchaseRepository purchases,ProductRepository products,SupplierRepository suppliers,InventoryMovementRepository movements,OperationSupport ops){this.purchases=purchases;this.products=products;this.suppliers=suppliers;this.movements=movements;this.ops=ops;}

 public PurchaseResponse create(PurchaseRequest request,String actor){
  var fingerprint=ops.request(request.requestId(),request);
  var previous=purchases.findByRequestId(request.requestId());
  if(previous.isPresent()){
   if(!fingerprint.equals(previous.get().getRequestFingerprint()))throw new BusinessException("La solicitud ya registró otra compra. Consulta el historial.");
   return map(previous.get());
  }
  ops.date(request.date());
  if(request.items()==null||request.items().isEmpty()||request.items().size()>100)throw new BusinessException("Agrega entre 1 y 100 productos.");
  var ids=new TreeSet<Long>();
  for(var line:request.items())if(line==null||line.productId()==null||line.quantity()<1||!ids.add(line.productId()))throw new BusinessException("Revisa las cantidades. Cada referencia debe aparecer una sola vez.");
  var locked=new HashMap<Long,Product>();
  for(var id:ids)locked.put(id,products.lockById(id).filter(p->!p.isDeleted()).orElseThrow(()->new ResourceNotFoundException("Producto no disponible: "+id)));
  var supplier=suppliers.findById(request.supplierId()).filter(Supplier::isActive).orElseThrow(()->new BusinessException("El proveedor seleccionado no está disponible."));
  var purchase=new Purchase();purchase.setSupplier(supplier);purchase.setSupplierName(supplier.getName());purchase.setReceivedOn(request.date());purchase.setPurchaseDate(ops.now());purchase.setReference(ops.clean(request.reference()));purchase.setNotes(ops.clean(request.notes()));purchase.setRecordedBy(actor);purchase.setRequestId(request.requestId());purchase.setRequestFingerprint(fingerprint);
  var total=BigDecimal.ZERO;
  for(var line:request.items()){
   var product=locked.get(line.productId());
   if(product.isProvisionalData())throw new BusinessException("Confirma los datos provisionales de "+product.getName()+" antes de recibir mercancía.");
   try{Math.addExact(product.getStock(),line.quantity());}catch(ArithmeticException e){throw new BusinessException("La compra excede la cantidad máxima de inventario.");}
   var item=new PurchaseItem();item.setPurchase(purchase);item.setProduct(product);item.setProductName(product.getName());item.setProductCode(product.getCode());item.setShade(product.getShade());item.setQuantity(line.quantity());item.setUnitCost(ops.amount(line.unitCost()));item.setSubtotal(ops.amount(line.unitCost().multiply(BigDecimal.valueOf(line.quantity()))));purchase.getItems().add(item);total=ops.amount(total.add(item.getSubtotal()));
  }
  purchase.setTotal(total);var saved=purchases.saveAndFlush(purchase);
  for(var item:saved.getItems()){var product=item.getProduct();product.setStock(product.getStock()+item.getQuantity());movement(product,MovementType.PURCHASE,item.getQuantity(),"Compra #"+saved.getId()+" · "+actor);}
  products.flush();return map(saved);
 }

 public PurchaseResponse cancel(Long id,String reason,String actor){
  var purchase=purchases.lockById(id).orElseThrow(()->new ResourceNotFoundException("Compra no encontrada"));
  if(purchase.isCancelled())return map(purchase);
  var explanation=ops.reason(reason);var quantities=new TreeMap<Long,Integer>();
  for(var item:purchase.getItems())quantities.merge(item.getProduct().getId(),item.getQuantity(),Math::addExact);
  // Products stay lazy until they are read under their inventory lock.
  for(var entry:quantities.entrySet()){
   var product=products.lockById(entry.getKey()).orElseThrow(()->new ResourceNotFoundException("Producto original no encontrado"));
   if(product.getStock()<entry.getValue())throw new BusinessException("No se puede anular: stock insuficiente para "+product.getName()+". Parte de las unidades pudo venderse o ajustarse.");
   product.setStock(product.getStock()-entry.getValue());movement(product,MovementType.ADJUSTMENT_OUT,entry.getValue(),"Anulación compra #"+purchase.getId()+" · "+actor);
  }
  purchase.setCancelled(true);purchase.setCancelledAt(ops.now());purchase.setCancelledBy(actor);purchase.setCancellationReason(explanation);purchases.flush();return map(purchase);
 }

 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public History<PurchaseResponse> list(String q,LocalDate from,LocalDate to,Boolean cancelled,int page,int size){
  ops.filters(from,to,page,size);var search=q==null?"":q.trim();
  var rows=purchases.search(search,from,to,cancelled,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"receivedOn","id")));
  var totals=purchases.totals(search,from,to,cancelled);
  return new History<>(rows.map(this::map).getContent(),rows.getTotalElements(),rows.getTotalPages(),rows.getNumber(),totals.getConfirmedTotal(),totals.getConfirmedCount(),totals.getCancelledCount());
 }
 @Transactional(readOnly=true) public PurchaseResponse get(Long id){return map(purchases.findById(id).orElseThrow(()->new ResourceNotFoundException("Compra no encontrada")));}
 private PurchaseResponse map(Purchase p){
  var lines=p.getItems().stream().map(i->new LineResponse(i.getProduct().getId(),i.getProductName()==null?i.getProduct().getName():i.getProductName(),i.getProductCode()==null?i.getProduct().getCode():i.getProductCode(),i.getProductName()==null?i.getProduct().getShade():i.getShade(),i.getQuantity(),i.getUnitCost(),i.getSubtotal())).toList();
  return new PurchaseResponse(p.getId(),p.getSupplierName()==null?p.getSupplier().getName():p.getSupplierName(),p.getReceivedOn()==null?p.getPurchaseDate().atZone(ZoneId.of("America/Bogota")).toLocalDate():p.getReceivedOn(),p.getPurchaseDate(),p.getReference(),p.getNotes(),p.getTotal(),p.getRecordedBy(),p.isCancelled(),p.getCancelledAt(),p.getCancellationReason(),p.getCancelledBy(),p.getSupplierName()!=null&&p.getItems().stream().allMatch(i->i.getProductName()!=null),lines);
 }
 private void movement(Product product,MovementType type,int quantity,String reference){var movement=new InventoryMovement();movement.setProduct(product);movement.setType(type);movement.setQuantity(quantity);movement.setResultingStock(product.getStock());movement.setReference(reference);movements.save(movement);}
}
