package com.bloomibunny.service;

import com.bloomibunny.dto.SaleDtos.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.exception.*;
import com.bloomibunny.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="BLOOMI_TEST_DB_URL",matches="jdbc:postgresql://[^?]+\\?currentSchema=bloomi_sales_test_[a-z0-9_]+")
@SpringBootTest(properties={"spring.datasource.url=${BLOOMI_TEST_DB_URL}","spring.datasource.username=${BLOOMI_TEST_DB_USERNAME}","spring.datasource.password=${BLOOMI_TEST_DB_PASSWORD}","spring.jpa.hibernate.ddl-auto=create-drop","app.bootstrap.admin-username=sales-integration-admin","app.bootstrap.admin-password=integration-fixture-password","jwt.secret=integration-only-signing-key-with-enough-bytes"})
@DirtiesContext
class SalePaymentIntegrationTest {
 @Autowired AdminSaleService sales;
 @Autowired SalePaymentService payments;
 @Autowired SalePaymentRepository paymentRows;
 @Autowired ProductRepository products;
 @Autowired CategoryRepository categories;
 @Autowired InventoryMovementRepository movements;
 @Autowired DashboardRepository dashboard;
 @Autowired JdbcTemplate jdbc;
 private final ZoneId zone=ZoneId.of("America/Bogota");
 @BeforeEach void reset(){assertTrue(jdbc.queryForObject("select current_schema()",String.class).startsWith("bloomi_sales_test_"));jdbc.execute("truncate table favorite,inventory_movement,sale_item,sale,product restart identity cascade");}
 LocalDate today(){return LocalDate.now(zone);}
 Product product(){var p=new Product();p.setName("Labial prueba");p.setCode("PAYMENT-TEST");p.setShade("Tono 2");p.setStock(10);p.setSalePrice(new BigDecimal("20000"));p.setPurchasePrice(new BigDecimal("8000"));p.setCategory(categories.findAll().getFirst());return products.saveAndFlush(p);}
 SaleResponse sale(){var p=product();return sales.create(new CreateSaleRequest(UUID.randomUUID(),null,PaymentMethod.TRANSFER,SaleChannel.WHATSAPP,BigDecimal.ZERO,"Prueba aislada",List.of(new SaleLineRequest(p.getId(),1,null))),"admin-sale");}
 PaymentRequest request(String amount){return new PaymentRequest(UUID.randomUUID(),new BigDecimal(amount),today(),PaymentMethod.TRANSFER,"Comprobante","Abono prueba");}
 SaleResponse pay(SaleResponse s,String amount){return payments.add(s.id(),request(amount),"admin-pay");}
 SaleResponse approve(SaleResponse s){return payments.authorize(s.id(),new CollectionRequest(s.version(),true,"Pago completo verificado"),"admin-approve");}
 void money(String expected,BigDecimal actual){assertNotNull(actual);assertEquals(0,new BigDecimal(expected).compareTo(actual));}
 BigDecimal collections(){return dashboard.authorizedCollections(today().atStartOfDay(zone).toInstant(),today().plusDays(1).atStartOfDay(zone).toInstant());}

 @Test void partialAndFullPaymentsNeverAutomaticallyAuthorizeCollection(){
  var s=sale();money("0",s.paidTotal());money("20000",s.balance());assertEquals("PENDING",s.paymentStatus());assertFalse(s.collectionAuthorized());
  var partial=pay(s,"5000");money("5000",partial.paidTotal());money("15000",partial.balance());assertEquals("PARTIAL",partial.paymentStatus());assertFalse(partial.collectionAuthorized());money("0",collections());
  assertThrows(BusinessException.class,()->approve(partial));var full=pay(partial,"15000");assertEquals("PAID",full.paymentStatus());assertFalse(full.collectionAuthorized());money("0",collections());
  var authorized=approve(full);assertTrue(authorized.collectionAuthorized());assertEquals("admin-approve",authorized.collectionAuthorizedBy());assertNotNull(authorized.collectionAuthorizedAt());money("20000",collections());
  assertEquals(9,products.findAll().getFirst().getStock());assertEquals(1,movements.count());money("8000",authorized.costTotal());
 }
 @Test void invalidAmountsDatesAndOverpaymentRollBack(){
  var s=sale();
  for(var value:List.of("0","-1","20000.01","0.001","1000000000000"))assertThrows(BusinessException.class,()->payments.add(s.id(),request(value),"admin"));
  assertThrows(BusinessException.class,()->payments.add(s.id(),new PaymentRequest(UUID.randomUUID(),BigDecimal.ONE,today().plusDays(1),PaymentMethod.CASH,null,null),"admin"));
  assertThrows(BusinessException.class,()->payments.add(s.id(),new PaymentRequest(UUID.randomUUID(),BigDecimal.ONE,today().minusDays(1),PaymentMethod.CASH,null,null),"admin"));
  assertEquals(0,paymentRows.count());money("20000",sales.get(s.id()).balance());
 }
 @Test void sameRequestRecoversPaymentAndOtherPayloadCannotReuseIt(){
  var s=sale();var input=request("1000");var first=payments.add(s.id(),input,"admin");var second=payments.add(s.id(),input,"admin");
  assertEquals(first.payments().getFirst().id(),second.payments().getFirst().id());assertEquals(1,paymentRows.count());money("1000",second.paidTotal());
  assertThrows(BusinessException.class,()->payments.add(s.id(),new PaymentRequest(input.requestId(),new BigDecimal("2000"),today(),PaymentMethod.TRANSFER,"Comprobante","Abono prueba"),"admin"));
 }
 @Test void cancellingPaymentRetainsAuditAndRevokesAuthorizationWithoutChangingStock(){
  var s=approve(pay(sale(),"20000"));var p=s.payments().getFirst();var result=payments.cancel(s.id(),p.id(),"Comprobante equivocado","admin-cancel");
  assertFalse(result.collectionAuthorized());money("0",collections());money("0",result.paidTotal());money("20000",result.balance());assertTrue(result.payments().getFirst().cancelled());assertEquals("admin-cancel",result.payments().getFirst().cancelledBy());assertEquals(2,result.collectionEvents().size());
  payments.cancel(s.id(),p.id(),"Reintento de anulación","admin");assertEquals(2,sales.get(s.id()).collectionEvents().size());assertEquals(1,movements.count());assertEquals(9,products.findAll().getFirst().getStock());
 }
 @Test void saleCancellationKeepsPaymentsButExcludesThemAndCollectionsFromIndicators(){
  var s=approve(pay(sale(),"20000"));money("20000",dashboard.recordedPayments(today(),today().plusDays(1)));var result=sales.cancel(s.id(),"Cliente cancela compra","admin");
  assertTrue(result.cancelled());assertFalse(result.collectionAuthorized());money("20000",result.paidTotal());money("0",collections());money("0",dashboard.recordedPayments(today(),today().plusDays(1)));money("0",dashboard.outstandingBalance());assertEquals(10,products.findAll().getFirst().getStock());
  assertThrows(BusinessException.class,()->pay(result,"1"));assertThrows(BusinessException.class,()->approve(result));assertThrows(BusinessException.class,()->payments.cancel(result.id(),result.payments().getFirst().id(),"Pago de venta anulada","admin"));
 }
 @Test void authorizationCanBeWithdrawnAndStaleVersionIsRejected(){
  var s=sale();var full=pay(s,"20000");assertThrows(BusinessException.class,()->approve(s));var authorized=approve(full);
  var result=payments.authorize(s.id(),new CollectionRequest(authorized.version(),false,"Revisar comprobante de pago"),"admin");assertFalse(result.collectionAuthorized());money("0",collections());money("20000",result.paidTotal());
 }
 @Test void legacyPaymentsAreUnknownUntilExplicitReviewAndNeverInvented(){
  var s=sale();jdbc.update("update sale set payments_reviewed=false where id=?",s.id());var legacy=sales.get(s.id());assertEquals("UNREVIEWED",legacy.paymentStatus());assertNull(legacy.balance());assertNull(legacy.paidTotal());assertEquals(1,dashboard.unreviewedPayments());money("0",dashboard.outstandingBalance());
  assertThrows(BusinessException.class,()->pay(legacy,"1"));assertThrows(BusinessException.class,()->approve(legacy));
  var reviewed=payments.review(s.id(),new ReviewPaymentsRequest(legacy.version(),"Comprobantes anteriores revisados"),"admin");money("20000",reviewed.balance());money("0",reviewed.paidTotal());assertEquals(0,dashboard.unreviewedPayments());assertEquals("REVIEWED",reviewed.collectionEvents().getFirst().action());
 }
 @Test void filtersDistinguishPartialPaidAndAuthorizedAcrossWholeHistory(){
  var s=sale();assertEquals(1,sales.list("",null,null,null,null,0,20,"PENDING").getTotalElements());var partial=pay(s,"5000");assertEquals(1,sales.list("",null,null,null,null,0,20,"PARTIAL").getTotalElements());
  var full=pay(partial,"15000");assertEquals(1,sales.list("",null,null,null,null,0,20,"PAID").getTotalElements());approve(full);assertEquals(0,sales.list("",null,null,null,null,0,20,"PAID").getTotalElements());assertEquals(1,sales.list("",null,null,null,null,0,20,"AUTHORIZED").getTotalElements());
 }
 @Test void paymentDateAndAuthorizationDateAreIndependentFromSaleDate(){
  var s=sale();var earlier=today().minusMonths(1).atStartOfDay(zone).toInstant();jdbc.update("update sale set sale_date=? where id=?",Timestamp.from(earlier),s.id());
  var input=new PaymentRequest(UUID.randomUUID(),new BigDecimal("20000"),today().minusDays(1),PaymentMethod.CASH,null,null);var full=payments.add(s.id(),input,"admin");approve(full);
  money("20000",collections());money("0",dashboard.recordedPayments(today(),today().plusDays(1)));money("20000",dashboard.recordedPayments(today().minusDays(1),today()));
 }
 @Test void concurrentDifferentPaymentsCannotExceedBalance()throws Exception{
  var s=sale();var start=new CountDownLatch(1);
  try(var executor=Executors.newFixedThreadPool(2)){
   Callable<Boolean> task=()->{start.await();try{pay(s,"15000");return true;}catch(BusinessException expected){return false;}};
   var first=executor.submit(task);var second=executor.submit(task);start.countDown();assertNotEquals(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS));
  }
  money("15000",sales.get(s.id()).paidTotal());assertEquals(1,paymentRows.count());
 }
 @Test void concurrentRetryRegistersOnlyOnePayment()throws Exception{
  var s=sale();var input=request("15000");var start=new CountDownLatch(1);
  try(var executor=Executors.newFixedThreadPool(2)){
   var first=executor.submit(()->{start.await();return payments.add(s.id(),input,"admin");});var second=executor.submit(()->{start.await();return payments.add(s.id(),input,"admin");});start.countDown();assertEquals(first.get(20,TimeUnit.SECONDS).payments().getFirst().id(),second.get(20,TimeUnit.SECONDS).payments().getFirst().id());
  }
  assertEquals(1,paymentRows.count());money("5000",sales.get(s.id()).balance());
 }
 @Test void cancellingPaymentAndAuthorizingConcurrentlyCannotLeaveInvalidAuthorization()throws Exception{
  var s=pay(sale(),"20000");var start=new CountDownLatch(1);
  try(var executor=Executors.newFixedThreadPool(2)){
   var first=executor.submit(()->{start.await();return payments.cancel(s.id(),s.payments().getFirst().id(),"Abono registrado por error","admin");});
   var second=executor.submit(()->{start.await();try{approve(s);}catch(BusinessException expected){}return true;});
   start.countDown();first.get(20,TimeUnit.SECONDS);second.get(20,TimeUnit.SECONDS);
  }
  var result=sales.get(s.id());assertFalse(result.collectionAuthorized());money("20000",result.balance());money("0",collections());
 }
 CreateSaleRequest initial(Product p,InitialPaymentStatus status,String amount){return new CreateSaleRequest(UUID.randomUUID(),null,PaymentMethod.CASH,SaleChannel.WHATSAPP,BigDecimal.ZERO,"Venta con pago inicial",List.of(new SaleLineRequest(p.getId(),1,null)),status,amount==null?null:new BigDecimal(amount));}
 @Test void creationCanExplicitlyStartWithPendingPayment(){
  var p=product();var result=sales.create(initial(p,InitialPaymentStatus.PENDING,"0"),"admin");assertEquals("PENDING",result.paymentStatus());money("20000",result.balance());assertTrue(result.payments().isEmpty());assertFalse(result.collectionAuthorized());
 }
 @Test void creationStoresPartialPaymentAndInventoryAtomically(){
  var p=product();var result=sales.create(initial(p,InitialPaymentStatus.PARTIAL,"5000"),"admin");assertEquals("PARTIAL",result.paymentStatus());money("5000",result.paidTotal());money("15000",result.balance());assertEquals(PaymentMethod.CASH,result.payments().getFirst().paymentMethod());assertEquals("admin",result.payments().getFirst().recordedBy());assertEquals(9,products.findById(p.getId()).orElseThrow().getStock());assertFalse(result.collectionAuthorized());money("0",collections());
 }
 @Test void creationStoresFullPaymentButRequiresSeparateCollectionAuthorization(){
  var p=product();var result=sales.create(initial(p,InitialPaymentStatus.PAID,null),"admin");assertEquals("PAID",result.paymentStatus());money("20000",result.paidTotal());money("0",result.balance());assertEquals(1,result.payments().size());assertFalse(result.collectionAuthorized());money("0",collections());approve(result);money("20000",collections());
 }
 @Test void invalidInitialPaymentRollsBackEntireSaleAndStock(){
  var p=product();
  for(var amount:List.of("0","20000","20001","0.001","-1"))assertThrows(BusinessException.class,()->sales.create(initial(p,InitialPaymentStatus.PARTIAL,amount),"admin"));
  assertThrows(BusinessException.class,()->sales.create(initial(p,InitialPaymentStatus.PENDING,"1"),"admin"));assertThrows(BusinessException.class,()->sales.create(initial(p,InitialPaymentStatus.PAID,"19999"),"admin"));
  assertEquals(0,paymentRows.count());assertEquals(0,movements.count());assertEquals(10,products.findById(p.getId()).orElseThrow().getStock());
 }
 @Test void initialPaymentRetryDoesNotDuplicatePaymentOrSale(){
  var p=product();var input=initial(p,InitialPaymentStatus.PARTIAL,"5000");var first=sales.create(input,"admin");var second=sales.create(input,"admin");assertEquals(first.id(),second.id());assertEquals(first.payments().getFirst().id(),second.payments().getFirst().id());assertEquals(1,paymentRows.count());assertEquals(1,movements.count());
 }
 @Test void concurrentInitialPaymentRetryPersistsOnlyOnce()throws Exception{
  var p=product();var input=initial(p,InitialPaymentStatus.PAID,"20000");var start=new CountDownLatch(1);
  try(var executor=Executors.newFixedThreadPool(2)){
   var first=executor.submit(()->{start.await();return sales.create(input,"admin");});var second=executor.submit(()->{start.await();return sales.create(input,"admin");});start.countDown();assertEquals(first.get(20,TimeUnit.SECONDS).id(),second.get(20,TimeUnit.SECONDS).id());
  }
  assertEquals(1,paymentRows.count());assertEquals(1,movements.count());assertEquals(9,products.findById(p.getId()).orElseThrow().getStock());
 }
}
