package com.bloomibunny.service;

import com.bloomibunny.entity.*;
import com.bloomibunny.exception.BusinessException;
import com.bloomibunny.repository.InventoryLotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @Transactional
public class InventoryLotService {
 private final InventoryLotRepository lots;
 private final OperationSupport ops;
 public InventoryLotService(InventoryLotRepository lots,OperationSupport ops){this.lots=lots;this.ops=ops;}
 public record LotResponse(String sourceKey,String reference,LocalDate date,int remaining,BigDecimal unitCost,BigDecimal costValue,boolean opening){}
 public record Valuation(BigDecimal knownCost,long unknownUnits){}
 private List<InventoryLot> queue(Product p){return lots.findByProductIdOrderByOpeningDescReceivedOnAscReceivedAtAscSourceKeyAsc(p.getId());}
 // Every mutation is called while its product is locked (or is newly created).
 public void ensure(Product p){
  var rows=queue(p);
  if(rows.isEmpty()){
   add(p,"opening","Saldo inicial",true,ops.now().atZone(ZoneId.of("America/Bogota")).toLocalDate(),ops.now(),p.getStock(),p.isCostConfirmed()?p.getPurchasePrice():null);
  }else if(rows.stream().mapToLong(InventoryLot::getRemaining).sum()!=p.getStock())throw new BusinessException("Las unidades de los lotes no coinciden con el inventario. Actualiza y revisa el producto.");
 }
 private void add(Product p,String key,String reference,boolean opening,LocalDate date,Instant at,int quantity,BigDecimal cost){
  var row=new InventoryLot();row.setProduct(p);row.setSourceKey(key);row.setReference(reference);row.setOpening(opening);row.setReceivedOn(date);row.setReceivedAt(at);row.setRemaining(quantity);row.setUnitCost(cost);lots.saveAndFlush(row);
 }
 public void receive(Product p,Purchase purchase,PurchaseItem item){
  ensure(p);add(p,"purchase:"+item.getId(),"Compra #"+purchase.getId(),false,purchase.getReceivedOn(),purchase.getPurchaseDate(),item.getQuantity(),item.getUnitCost());prune(p);
 }
 public void cancelReceipt(Product p,PurchaseItem item){
  ensure(p);var rows=queue(p);var row=rows.stream().filter(l->l.getSourceKey().equals("purchase:"+item.getId())).findFirst().orElseThrow(()->new BusinessException("No se puede anular: el lote de esta compra ya se consumió."));
  if(row.getRemaining()!=item.getQuantity())throw new BusinessException("No se puede anular: ya se vendieron o ajustaron unidades de este lote. Corrige primero esas operaciones.");
  row.setRemaining(0);lots.flush();prune(p);
 }
 public List<SaleCostAllocation> consume(Product p,int quantity){
  ensure(p);if(quantity<1||quantity>p.getStock())throw new BusinessException("Stock insuficiente para descontar los lotes.");
  var result=new ArrayList<SaleCostAllocation>();int left=quantity;
  for(var row:queue(p)){
   int take=Math.min(left,row.getRemaining());if(take==0)continue;
   var part=new SaleCostAllocation();part.setSourceKey(row.getSourceKey());part.setReference(row.getReference());part.setOpening(row.isOpening());part.setReceivedOn(row.getReceivedOn());part.setReceivedAt(row.getReceivedAt());part.setQuantity(take);part.setUnitCost(row.getUnitCost());result.add(part);
   row.setRemaining(row.getRemaining()-take);left-=take;if(left==0)break;
  }
  if(left!=0)throw new BusinessException("El inventario de lotes está incompleto.");
  lots.flush();prune(p);return result;
 }
 public void restore(Product p,SaleItem item){
  ensure(p);
  if(item.getCostAllocations().isEmpty()){
   // Legacy sales have no reconstructed origin; preserve their known/unknown historical cost.
   add(p,"legacy-return:"+item.getId(),"Devolución de venta anterior #"+item.getSale().getId(),false,ops.now().atZone(ZoneId.of("America/Bogota")).toLocalDate(),ops.now(),item.getQuantity(),item.getUnitCost());
  }else for(var part:item.getCostAllocations()){
   var row=queue(p).stream().filter(l->l.getSourceKey().equals(part.getSourceKey())).findFirst();
   if(row.isPresent())row.get().setRemaining(Math.addExact(row.get().getRemaining(),part.getQuantity()));
   else add(p,part.getSourceKey(),part.getReference(),part.isOpening(),part.getReceivedOn(),part.getReceivedAt(),part.getQuantity(),part.getUnitCost());
  }
  lots.flush();prune(p);
 }
 public void adjustment(Product p,int delta,BigDecimal cost){
  if(delta==0)return;
  if(delta<0)consume(p,-delta);
  else {ensure(p);add(p,"adjustment:"+UUID.randomUUID(),"Entrada por ajuste",false,ops.now().atZone(ZoneId.of("America/Bogota")).toLocalDate(),ops.now(),delta,cost);prune(p);}
 }
 private void prune(Product p){
  var rows=queue(p);var positive=rows.stream().filter(l->l.getRemaining()>0).toList();
  // Delete exhausted live rows; if everything is empty retain only the final row.
  var keep=positive.isEmpty()&&!rows.isEmpty()?rows.getLast():null;
  for(var row:rows)if(row.getRemaining()==0&&row!=keep)lots.delete(row);
  lots.flush();
 }
 @Transactional(readOnly=true) public List<LotResponse> list(Product p){return queue(p).stream().map(l->new LotResponse(l.getSourceKey(),l.getReference(),l.getReceivedOn(),l.getRemaining(),l.getUnitCost(),l.getUnitCost()==null?null:l.getUnitCost().multiply(BigDecimal.valueOf(l.getRemaining())),l.isOpening())).toList();}
 @Transactional(readOnly=true) public Map<Long,Valuation> values(List<Product> products){
  var grouped=new HashMap<Long,List<InventoryLot>>();
  if(!products.isEmpty())for(var l:lots.findByProductIdIn(products.stream().map(Product::getId).toList()))grouped.computeIfAbsent(l.getProduct().getId(),k->new ArrayList<>()).add(l);
  var result=new HashMap<Long,Valuation>();
  for(var p:products){var rows=grouped.get(p.getId());BigDecimal cost=BigDecimal.ZERO;long unknown=0;
   if(rows==null){if(p.isCostConfirmed())cost=p.getPurchasePrice().multiply(BigDecimal.valueOf(p.getStock()));else unknown=p.getStock();}
   else {if(rows.stream().mapToLong(InventoryLot::getRemaining).sum()!=p.getStock())throw new BusinessException("Las unidades de los lotes no coinciden con el inventario.");for(var row:rows)if(row.getUnitCost()==null)unknown+=row.getRemaining();else cost=cost.add(row.getUnitCost().multiply(BigDecimal.valueOf(row.getRemaining())));}
   result.put(p.getId(),new Valuation(cost,unknown));
  }return result;
 }
 public static BigDecimal totalCost(SaleItem item){return item.getCostTotal()!=null?item.getCostTotal():item.getUnitCost()==null?null:item.getUnitCost().multiply(BigDecimal.valueOf(item.getQuantity()));}
}
