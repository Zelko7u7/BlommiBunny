package com.bloomibunny.service;

import com.bloomibunny.dto.TransactionDtos.*;
import com.bloomibunny.dto.SaleDtos.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.exception.BusinessException;
import com.bloomibunny.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="BLOOMI_TEST_DB_URL",matches="jdbc:postgresql://[^?]+\\?currentSchema=bloomi_sales_test_[a-z0-9_]+")
@SpringBootTest(properties={"spring.datasource.url=${BLOOMI_TEST_DB_URL}","spring.datasource.username=${BLOOMI_TEST_DB_USERNAME}","spring.datasource.password=${BLOOMI_TEST_DB_PASSWORD}","spring.jpa.hibernate.ddl-auto=create-drop","app.bootstrap.admin-username=sales-integration-admin","app.bootstrap.admin-password=integration-fixture-password","jwt.secret=integration-only-signing-key-with-enough-bytes"})
@DirtiesContext
class PurchasesExpensesIntegrationTest {
 @Autowired AdminPurchaseService purchases;
 @Autowired AdminExpenseService expenses;
 @Autowired AdminSaleService sales;
 @Autowired PurchaseRepository purchaseRows;
 @Autowired ExpenseRepository expenseRows;
 @Autowired ProductRepository products;
 @Autowired SupplierRepository suppliers;
 @Autowired CategoryRepository categories;
 @Autowired InventoryMovementRepository movements;
 @Autowired JdbcTemplate jdbc;
 Supplier supplier;
 final LocalDate day=LocalDate.of(2026,1,15);
 @BeforeEach void reset(){assertTrue(jdbc.queryForObject("select current_schema()",String.class).startsWith("bloomi_sales_test_"));jdbc.execute("truncate table favorite,inventory_movement,sale_item,sale,purchase_item,purchase,expense,product,supplier restart identity cascade");supplier=new Supplier();supplier.setName("Proveedor original");supplier=suppliers.saveAndFlush(supplier);}
 Product product(String code,int stock){var p=new Product();p.setCode(code);p.setName("Producto "+code);p.setShade("Tono "+code);p.setStock(stock);p.setSalePrice(new BigDecimal("20000"));p.setPurchasePrice(new BigDecimal("8000"));p.setCategory(categories.findAll().getFirst());return products.saveAndFlush(p);}
 PurchaseRequest request(UUID key,Product p,int qty){return new PurchaseRequest(key,supplier.getId(),day,"FAC-TEST","Nota de recepción",List.of(new LineRequest(p.getId(),qty,new BigDecimal("11000"))));}
 ExpenseRequest expense(UUID key,LocalDate date,String amount){return new ExpenseRequest(key,"Publicidad","Campaña de prueba",new BigDecimal(amount),date,PaymentMethod.TRANSFER,"COMP-TEST","Nota completa");}
 int stock(Product p){return products.findById(p.getId()).orElseThrow().getStock();}
 @Test void purchaseStoresSnapshotsAndReceivesHiddenStockWithoutChangingReferenceCost(){
  var p=product("A",2);p.setActive(false);products.saveAndFlush(p);var result=purchases.create(request(UUID.randomUUID(),p,3),"administradora");assertEquals(5,stock(p));assertEquals(0,new BigDecimal("33000").compareTo(result.total()));assertEquals(day,result.date());assertEquals("administradora",result.recordedBy());assertEquals("FAC-TEST",result.reference());assertTrue(result.historicalSnapshot());assertEquals(0,new BigDecimal("8000").compareTo(products.findById(p.getId()).orElseThrow().getPurchasePrice()));
  p=products.findById(p.getId()).orElseThrow();p.setName("Nombre nuevo");p.setShade("Otro tono");products.saveAndFlush(p);supplier.setName("Otro proveedor");suppliers.saveAndFlush(supplier);var history=purchases.get(result.id());assertEquals("Proveedor original",history.supplier());assertEquals("Producto A",history.items().getFirst().product());assertEquals("Tono A",history.items().getFirst().shade());assertEquals(1,movements.count());
 }
 @Test void duplicateRequestDoesNotReceiveTwiceAndMismatchedPayloadIsRejected(){var p=product("A",0);var key=UUID.randomUUID();var result=purchases.create(request(key,p,2),"admin");assertEquals(result.id(),purchases.create(request(key,p,2),"admin").id());assertThrows(BusinessException.class,()->purchases.create(request(key,p,3),"admin"));assertEquals(2,stock(p));assertEquals(1,purchaseRows.count());assertEquals(1,movements.count());}
 @Test void invalidDuplicateAndUnconfirmedProductsDoNotChangeStock(){
  var p=product("A",2);var line=new LineRequest(p.getId(),2,new BigDecimal("500"));assertThrows(BusinessException.class,()->purchases.create(new PurchaseRequest(UUID.randomUUID(),supplier.getId(),day,null,null,List.of(line,line)),"admin"));
  p.setProvisionalData(true);products.saveAndFlush(p);assertThrows(BusinessException.class,()->purchases.create(request(UUID.randomUUID(),p,1),"admin"));assertEquals(2,stock(p));assertEquals(0,purchaseRows.count());assertEquals(0,movements.count());
 }
 @Test void purchaseCancellationRemovesUnitsOnceAndExcludesTotals(){var p=product("A",1);var result=purchases.create(request(UUID.randomUUID(),p,2),"admin");purchases.cancel(result.id(),"Recepción duplicada","admin-cancel");purchases.cancel(result.id(),"Recepción duplicada","admin-cancel");assertEquals(1,stock(p));assertEquals(2,movements.count());var history=purchases.list("",day,day,null,0,20);assertEquals(1,history.cancelledCount());assertEquals(0,history.confirmedCount());assertEquals(0,history.confirmedTotal().signum());assertEquals("admin-cancel",purchases.get(result.id()).cancelledBy());}
 @Test void insufficientStockRollsBackAllCancellationLines(){
  var a=product("A",0);var b=product("B",0);var result=purchases.create(new PurchaseRequest(UUID.randomUUID(),supplier.getId(),day,null,null,List.of(new LineRequest(a.getId(),2,BigDecimal.ONE),new LineRequest(b.getId(),2,BigDecimal.ONE))),"admin");b=products.findById(b.getId()).orElseThrow();b.setStock(0);products.saveAndFlush(b);assertThrows(BusinessException.class,()->purchases.cancel(result.id(),"Fallo de recepción","admin"));assertEquals(2,stock(a));assertFalse(purchases.get(result.id()).cancelled());assertEquals(2,movements.count());
 }
 @Test void concurrentReceiptsDoNotLoseInventory(){var p=product("A",0);parallel(()->purchases.create(request(UUID.randomUUID(),p,2),"one"),()->purchases.create(request(UUID.randomUUID(),p,3),"two"));assertEquals(5,stock(p));assertEquals(2,purchaseRows.count());}
 @Test void concurrentSameRequestReceivesOnlyOnce(){var p=product("A",0);var input=request(UUID.randomUUID(),p,2);parallel(()->purchases.create(input,"one"),()->purchases.create(input,"two"));assertEquals(2,stock(p));assertEquals(1,purchaseRows.count());assertEquals(1,movements.count());}
 @Test void cancellationAndSaleCannotRemoveTheSameLastUnits(){
  var p=product("A",0);var purchase=purchases.create(request(UUID.randomUUID(),p,2),"admin");var successes=new java.util.concurrent.atomic.AtomicInteger();
  parallel(()->{try{purchases.cancel(purchase.id(),"Compra no corresponde","admin");successes.incrementAndGet();}catch(BusinessException ignored){}return true;},()->{try{sales.create(new CreateSaleRequest(UUID.randomUUID(),null,PaymentMethod.CASH,SaleChannel.OTHER,BigDecimal.ZERO,null,List.of(new SaleLineRequest(p.getId(),2,null))),"admin");successes.incrementAndGet();}catch(BusinessException ignored){}return true;});assertEquals(1,successes.get());assertEquals(0,stock(p));
 }
 @Test void expenseStoresAllFieldsAndDoesNotAffectInventory(){var p=product("A",5);var result=expenses.create(expense(UUID.randomUUID(),day,"12500.50"),"administradora");assertEquals(5,stock(p));assertEquals(0,movements.count());assertEquals(PaymentMethod.TRANSFER,result.paymentMethod());assertEquals("COMP-TEST",result.reference());assertEquals("Nota completa",result.notes());assertEquals("administradora",result.recordedBy());assertEquals(day,result.date());assertEquals(0,new BigDecimal("12500.50").compareTo(result.amount()));}
 @Test void expenseRetryAndCancellationKeepHistoryWithoutDoubleTotals(){var key=UUID.randomUUID();var result=expenses.create(expense(key,day,"500"),"admin");assertEquals(result.id(),expenses.create(expense(key,day,"500"),"admin").id());assertThrows(BusinessException.class,()->expenses.create(expense(key,day,"600"),"admin"));expenses.cancel(result.id(),"Concepto incorrecto","admin");expenses.cancel(result.id(),"Concepto incorrecto","admin");var history=expenses.list("",day,day,null,0,20);assertEquals(1,history.totalElements());assertEquals(1,history.cancelledCount());assertEquals(0,history.confirmedTotal().signum());assertEquals(0,expenseRows.total(day,day).signum());}
 @Test void totalsCoverEveryPageAndDateFiltersAreInclusive(){var p=product("A",0);for(int i=0;i<3;i++){expenses.create(expense(UUID.randomUUID(),day.plusDays(i),"100"),"admin");purchases.create(new PurchaseRequest(UUID.randomUUID(),supplier.getId(),day.plusDays(i),"INV-"+i,null,List.of(new LineRequest(p.getId(),1,BigDecimal.TEN))),"admin");}var history=expenses.list("Publicidad",day,day.plusDays(1),false,0,1);assertEquals(2,history.totalElements());assertEquals(1,history.content().size());assertEquals(0,new BigDecimal("200").compareTo(history.confirmedTotal()));var buys=purchases.list("INV",day,day.plusDays(1),false,0,1);assertEquals(2,buys.totalElements());assertEquals(0,new BigDecimal("20").compareTo(buys.confirmedTotal()));assertEquals(0,purchases.list("no-existe",null,null,null,0,20).totalElements());}
 @Test void futureDatesInvalidMoneyInactiveSupplierAndOverflowAreRejected(){
  var p=product("A",Integer.MAX_VALUE);assertThrows(BusinessException.class,()->purchases.create(request(UUID.randomUUID(),p,1),"admin"));assertThrows(BusinessException.class,()->expenses.create(expense(UUID.randomUUID(),LocalDate.now().plusYears(1),"100"),"admin"));assertThrows(BusinessException.class,()->expenses.create(expense(UUID.randomUUID(),day,"0"),"admin"));assertThrows(BusinessException.class,()->expenses.create(expense(UUID.randomUUID(),day,"1.001"),"admin"));supplier.setActive(false);suppliers.saveAndFlush(supplier);assertThrows(BusinessException.class,()->purchases.create(request(UUID.randomUUID(),p,1),"admin"));assertEquals(Integer.MAX_VALUE,stock(p));assertEquals(0,purchaseRows.count());assertEquals(0,expenseRows.count());
 }
 @Test void repeatedConcurrentCancellationsOnlyRemoveStockOnce(){var p=product("A",0);var result=purchases.create(request(UUID.randomUUID(),p,2),"admin");parallel(()->purchases.cancel(result.id(),"Duplicación detectada","one"),()->purchases.cancel(result.id(),"Duplicación detectada","two"));assertEquals(0,stock(p));assertEquals(2,movements.count());}
 @Test void invalidReasonAndDateRangePreserveRecords(){var result=expenses.create(expense(UUID.randomUUID(),day,"100"),"admin");assertThrows(BusinessException.class,()->expenses.cancel(result.id(),"   ","admin"));assertFalse(expenses.get(result.id()).cancelled());assertThrows(BusinessException.class,()->expenses.list("",day.plusDays(1),day,null,0,20));assertThrows(BusinessException.class,()->purchases.list("",null,null,null,-1,20));}
 void parallel(Callable<?> a,Callable<?> b){var gate=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(2)){var one=pool.submit(()->{gate.await();return a.call();});var two=pool.submit(()->{gate.await();return b.call();});gate.countDown();one.get(15,TimeUnit.SECONDS);two.get(15,TimeUnit.SECONDS);}catch(Exception e){throw new RuntimeException(e);}}
}
