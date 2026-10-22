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
 private final SaleRepository sales;private final PurchaseRepository purchases;private final ProductRepository products;private final CustomerRepository customers;private final SupplierRepository suppliers;private final InventoryMovementRepository movements;
 public TransactionServiceImpl(SaleRepository a,PurchaseRepository b,ProductRepository c,CustomerRepository d,SupplierRepository e,InventoryMovementRepository f){sales=a;purchases=b;products=c;customers=d;suppliers=e;movements=f;}
 public SaleResponse sale(SaleRequest r){var s=new Sale();s.setCustomer(r.customerId()==null?null:customers.findById(r.customerId()).orElseThrow(()->new ResourceNotFoundException("Cliente no encontrado")));s.setPaymentMethod(r.paymentMethod());BigDecimal subtotal=BigDecimal.ZERO;
  for(var line:r.items()){var p=product(line.productId());if(p.getStock()<line.quantity())throw new BusinessException("Stock insuficiente para "+p.getName());var price=line.unitPrice()==null?p.getSalePrice():line.unitPrice();var item=new SaleItem();item.setSale(s);item.setProduct(p);item.setQuantity(line.quantity());item.setUnitPrice(price);item.setSubtotal(price.multiply(BigDecimal.valueOf(line.quantity())));s.getItems().add(item);subtotal=subtotal.add(item.getSubtotal());p.setStock(p.getStock()-line.quantity());move(p,MovementType.SALE,line.quantity(),"Venta");}
  s.setSubtotal(subtotal);s.setDiscount(r.discount()==null?BigDecimal.ZERO:r.discount());s.setTotal(subtotal.subtract(s.getDiscount()).max(BigDecimal.ZERO));return saleMap(sales.save(s));}
 public PurchaseResponse purchase(PurchaseRequest r){var pch=new Purchase();pch.setSupplier(suppliers.findById(r.supplierId()).orElseThrow(()->new ResourceNotFoundException("Proveedor no encontrado")));BigDecimal total=BigDecimal.ZERO;
  for(var line:r.items()){var p=product(line.productId());var cost=line.unitPrice()==null?p.getPurchasePrice():line.unitPrice();var item=new PurchaseItem();item.setPurchase(pch);item.setProduct(p);item.setQuantity(line.quantity());item.setUnitCost(cost);item.setSubtotal(cost.multiply(BigDecimal.valueOf(line.quantity())));pch.getItems().add(item);total=total.add(item.getSubtotal());p.setStock(p.getStock()+line.quantity());p.setPurchasePrice(cost);move(p,MovementType.PURCHASE,line.quantity(),"Compra");}
  pch.setTotal(total);return purchaseMap(purchases.save(pch));}
 private Product product(Long id){return products.findById(id).filter(p->!p.isDeleted()).orElseThrow(()->new ResourceNotFoundException("Producto no encontrado"));}
 private void move(Product p,MovementType t,int q,String ref){products.save(p);var m=new InventoryMovement();m.setProduct(p);m.setType(t);m.setQuantity(q);m.setResultingStock(p.getStock());m.setReference(ref);movements.save(m);}
 private SaleResponse saleMap(Sale s){return new SaleResponse(s.getId(),s.getCustomer()==null?"Consumidor final":s.getCustomer().getName(),s.getSaleDate(),s.getPaymentMethod(),s.getSubtotal(),s.getDiscount(),s.getTotal(),s.getItems().stream().map(i->new LineResponse(i.getProduct().getId(),i.getProduct().getName(),i.getQuantity(),i.getUnitPrice(),i.getSubtotal())).toList());}
 private PurchaseResponse purchaseMap(Purchase p){return new PurchaseResponse(p.getId(),p.getSupplier().getName(),p.getPurchaseDate(),p.getTotal(),p.getItems().stream().map(i->new LineResponse(i.getProduct().getId(),i.getProduct().getName(),i.getQuantity(),i.getUnitCost(),i.getSubtotal())).toList());}
}
