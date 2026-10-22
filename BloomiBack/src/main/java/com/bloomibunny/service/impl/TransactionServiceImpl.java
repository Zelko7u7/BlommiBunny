package com.bloomibunny.service.impl;
import com.bloomibunny.dto.TransactionDtos.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.exception.*;
import com.bloomibunny.repository.*;
import com.bloomibunny.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
@Service @Transactional
public class TransactionServiceImpl implements TransactionService {
 private final PurchaseRepository purchases;private final ProductRepository products;private final SupplierRepository suppliers;private final InventoryMovementRepository movements;
 public TransactionServiceImpl(PurchaseRepository b,ProductRepository c,SupplierRepository e,InventoryMovementRepository f){purchases=b;products=c;suppliers=e;movements=f;}
 public PurchaseResponse purchase(PurchaseRequest r){var pch=new Purchase();pch.setSupplier(suppliers.findById(r.supplierId()).orElseThrow(()->new ResourceNotFoundException("Proveedor no encontrado")));BigDecimal total=BigDecimal.ZERO;
  for(var line:r.items()){var p=product(line.productId());var cost=line.unitPrice()==null?p.getPurchasePrice():line.unitPrice();var item=new PurchaseItem();item.setPurchase(pch);item.setProduct(p);item.setQuantity(line.quantity());item.setUnitCost(cost);item.setSubtotal(cost.multiply(BigDecimal.valueOf(line.quantity())));pch.getItems().add(item);total=total.add(item.getSubtotal());p.setStock(p.getStock()+line.quantity());p.setPurchasePrice(cost);move(p,MovementType.PURCHASE,line.quantity(),"Compra");}
  pch.setTotal(total);return purchaseMap(purchases.save(pch));}
 private Product product(Long id){return products.findById(id).filter(p->!p.isDeleted()).orElseThrow(()->new ResourceNotFoundException("Producto no encontrado"));}
 private void move(Product p,MovementType t,int q,String ref){products.save(p);var m=new InventoryMovement();m.setProduct(p);m.setType(t);m.setQuantity(q);m.setResultingStock(p.getStock());m.setReference(ref);movements.save(m);}
 private PurchaseResponse purchaseMap(Purchase p){return new PurchaseResponse(p.getId(),p.getSupplier().getName(),p.getPurchaseDate(),p.getTotal(),p.getItems().stream().map(i->new LineResponse(i.getProduct().getId(),i.getProduct().getName(),i.getQuantity(),i.getUnitCost(),i.getSubtotal())).toList());}
}
