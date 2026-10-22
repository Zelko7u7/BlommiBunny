package com.bloomibunny.service;

import com.bloomibunny.dto.SaleDtos.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.exception.*;
import com.bloomibunny.repository.*;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service @Transactional
public class AdminSaleService {
 private static final BigDecimal MAX_AMOUNT=new BigDecimal("999999999999.99");
 private static final ZoneId BUSINESS_ZONE=ZoneId.of("America/Bogota");
 private final SaleRepository sales;
 private final ProductRepository products;
 private final CustomerRepository customers;
 private final InventoryMovementRepository movements;
 private final JdbcTemplate jdbc;
 public AdminSaleService(SaleRepository sales,ProductRepository products,CustomerRepository customers,InventoryMovementRepository movements,JdbcTemplate jdbc){
  this.sales=sales;this.products=products;this.customers=customers;this.movements=movements;this.jdbc=jdbc;
 }

 public SaleResponse create(CreateSaleRequest request,String actor){
  // Serializes retries of the same request before any inventory is changed.
  var key=request.requestId();
  if(key==null)throw new BusinessException("La solicitud debe tener un identificador.");
  jdbc.queryForObject("select 1 from pg_advisory_xact_lock(?)",Integer.class,key.getMostSignificantBits()^key.getLeastSignificantBits());
  var fingerprint=fingerprint(request);
  var previous=sales.findByRequestId(key);
  if(previous.isPresent()){
   if(!fingerprint.equals(previous.get().getRequestFingerprint()))throw new BusinessException("Esta solicitud ya registró otra venta. Consulta el historial antes de continuar.");
   return map(previous.get());
  }
  if(request.items()==null||request.items().isEmpty()||request.items().size()>100)throw new BusinessException("Agrega entre 1 y 100 productos.");
  var ids=new TreeSet<Long>();
  for(var line:request.items())if(line==null||line.productId()==null||line.quantity()<1||!ids.add(line.productId()))throw new BusinessException("Revisa las cantidades. Cada producto debe aparecer una sola vez.");
  var locked=new HashMap<Long,Product>();
  for(var id:ids)locked.put(id,products.lockById(id).filter(p->!p.isDeleted()).orElseThrow(()->new ResourceNotFoundException("Producto no disponible: "+id)));
  var sale=new Sale();
  if(request.customerId()!=null)sale.setCustomer(customers.findById(request.customerId()).filter(Customer::isActive).orElseThrow(()->new BusinessException("El cliente seleccionado no está disponible.")));
  sale.setCustomerName(sale.getCustomer()==null?"Consumidor final":sale.getCustomer().getName());
  if(request.paymentMethod()==null||request.channel()==null)throw new BusinessException("Selecciona el método de pago y el canal de venta.");
  sale.setRequestId(key);sale.setRequestFingerprint(fingerprint);sale.setPaymentMethod(request.paymentMethod());sale.setChannel(request.channel());sale.setRecordedBy(actor);
  sale.setNotes(request.notes()==null?null:request.notes().trim());
  BigDecimal subtotal=BigDecimal.ZERO;
  for(var line:request.items()){
   var product=locked.get(line.productId());
   if(product.isProvisionalData())throw new BusinessException("Confirma los datos provisionales de "+product.getName()+" antes de venderlo.");
   if(product.getStock()<line.quantity())throw new BusinessException("Stock insuficiente para "+product.getName()+tone(product)+". Actualiza los productos y revisa la cantidad.");
   var price=amount(line.unitPrice()==null?product.getSalePrice():line.unitPrice());
   var item=new SaleItem();item.setSale(sale);item.setProduct(product);item.setQuantity(line.quantity());item.setUnitPrice(price);
   item.setSubtotal(amount(price.multiply(BigDecimal.valueOf(line.quantity()))));item.setProductName(product.getName());item.setProductCode(product.getCode());item.setShade(product.getShade());
   item.setUnitCost(product.isCostConfirmed()?product.getPurchasePrice():null);
   sale.getItems().add(item);subtotal=amount(subtotal.add(item.getSubtotal()));
  }
  var discount=amount(request.discount()==null?BigDecimal.ZERO:request.discount());
  if(discount.compareTo(subtotal)>0)throw new BusinessException("El descuento no puede superar el subtotal.");
  sale.setSubtotal(subtotal);sale.setDiscount(discount);sale.setTotal(subtotal.subtract(discount));
  var saved=sales.saveAndFlush(sale);
  for(var line:saved.getItems()){
   var product=line.getProduct();product.setStock(product.getStock()-line.getQuantity());
   movement(product,MovementType.SALE,line.getQuantity(),"Venta #"+saved.getId()+" · "+actor);
  }
  products.flush();
  return map(saved);
 }

 public SaleResponse cancel(Long id,String reason,String actor){
  var sale=sales.lockById(id).orElseThrow(()->new ResourceNotFoundException("Venta no encontrada"));
  if(sale.isCancelled())return map(sale);
  if(reason==null||reason.trim().length()<5||reason.trim().length()>500)throw new BusinessException("Explica el motivo de anulación (entre 5 y 500 caracteres).");
  var quantities=new TreeMap<Long,Integer>();
  for(var item:sale.getItems())quantities.merge(item.getProduct().getId(),item.getQuantity(),Math::addExact);
  for(var entry:quantities.entrySet()){
   var product=products.lockById(entry.getKey()).orElseThrow(()->new ResourceNotFoundException("No se encontró el producto original de la venta."));
   try{product.setStock(Math.addExact(product.getStock(),entry.getValue()));}catch(ArithmeticException ex){throw new BusinessException("La devolución excede la cantidad máxima del inventario.");}
   movement(product,MovementType.ADJUSTMENT_IN,entry.getValue(),"Anulación venta #"+sale.getId()+" · "+actor);
  }
  sale.setCancelled(true);sale.setCancelledAt(Instant.now());sale.setCancellationReason(reason.trim());sale.setCancelledBy(actor);
  sales.flush();return map(sale);
 }

 @Transactional(readOnly=true)
 public Page<SaleResponse> list(String q,LocalDate from,LocalDate to,Boolean cancelled,SaleChannel channel,int page,int size){
  if(page<0||size<1||size>100)throw new BusinessException("Paginación inválida.");
  if(from!=null&&to!=null&&from.isAfter(to))throw new BusinessException("La fecha inicial no puede superar la final.");
  return sales.search(q==null?"":q.trim(),from==null?null:from.atStartOfDay(BUSINESS_ZONE).toInstant(),to==null?null:to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant(),cancelled,channel,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"saleDate","id"))).map(this::map);
 }
 @Transactional(readOnly=true)
 public SaleResponse get(Long id){return map(sales.findById(id).orElseThrow(()->new ResourceNotFoundException("Venta no encontrada")));}

 private SaleResponse map(Sale sale){
  boolean snapshot=sale.getItems().stream().allMatch(item->item.getProductName()!=null);
  var lines=sale.getItems().stream().map(item->new SaleLine(item.getProduct().getId(),item.getProductName()==null?item.getProduct().getName():item.getProductName(),item.getProductCode()==null?item.getProduct().getCode():item.getProductCode(),item.getProductName()==null?item.getProduct().getShade():item.getShade(),item.getQuantity(),item.getUnitPrice(),item.getUnitCost(),item.getSubtotal())).toList();
  BigDecimal cost=lines.stream().anyMatch(line->line.unitCost()==null)?null:lines.stream().map(line->line.unitCost().multiply(BigDecimal.valueOf(line.quantity()))).reduce(BigDecimal.ZERO,BigDecimal::add);
  return new SaleResponse(sale.getId(),sale.getCustomerName()!=null?sale.getCustomerName():sale.getCustomer()==null?"Consumidor final":sale.getCustomer().getName(),sale.getSaleDate(),sale.getPaymentMethod(),sale.getChannel()==null?SaleChannel.OTHER:sale.getChannel(),sale.getSubtotal(),sale.getDiscount(),sale.getTotal(),cost,cost==null?null:sale.getTotal().subtract(cost),sale.getNotes(),sale.getRecordedBy(),sale.isCancelled(),sale.getCancelledAt(),sale.getCancellationReason(),sale.getCancelledBy(),snapshot,lines);
 }
 private BigDecimal amount(BigDecimal value){if(value.signum()<0||value.stripTrailingZeros().scale()>2||value.compareTo(MAX_AMOUNT)>0)throw new BusinessException("Revisa los importes: deben ser positivos o cero, con hasta dos decimales y dentro del límite permitido.");return value;}
 private String tone(Product product){return product.getShade()==null?"":" · "+product.getShade();}
 private void movement(Product product,MovementType type,int quantity,String reference){var movement=new InventoryMovement();movement.setProduct(product);movement.setType(type);movement.setQuantity(quantity);movement.setResultingStock(product.getStock());movement.setReference(reference);movements.save(movement);}
 private String fingerprint(CreateSaleRequest request){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(request.toString().getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
}
