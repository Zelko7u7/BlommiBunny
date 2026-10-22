package com.bloomibunny.service;

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
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

// Explicit opt-in: create-drop may only run in a dedicated test schema, never public.
@EnabledIfEnvironmentVariable(named="BLOOMI_TEST_DB_URL",matches="jdbc:postgresql://[^?]+\\?currentSchema=bloomi_sales_test_[a-z0-9_]+")
@SpringBootTest(properties={
 "spring.datasource.url=${BLOOMI_TEST_DB_URL}","spring.datasource.username=${BLOOMI_TEST_DB_USERNAME}","spring.datasource.password=${BLOOMI_TEST_DB_PASSWORD}",
 "spring.jpa.hibernate.ddl-auto=create-drop","app.bootstrap.admin-username=sales-integration-admin",
 "app.bootstrap.admin-password=integration-fixture-password","jwt.secret=integration-only-signing-key-with-enough-bytes"
})
@DirtiesContext
class AdminSaleIntegrationTest {
 @Autowired AdminSaleService service;
 @Autowired ProductRepository products;
 @Autowired SaleRepository sales;
 @Autowired CategoryRepository categories;
 @Autowired CustomerRepository customers;
 @Autowired InventoryMovementRepository movements;
 @Autowired JdbcTemplate jdbc;

 @BeforeEach void reset(){
  assertTrue(jdbc.queryForObject("select current_schema()",String.class).startsWith("bloomi_sales_test_"));
  jdbc.execute("truncate table favorite,inventory_movement,sale_item,sale,product restart identity cascade");
 }
 Product product(String code,int stock){var product=new Product();product.setName("Producto de prueba "+code);product.setCode(code);product.setCategory(categories.findAll().getFirst());product.setStock(stock);product.setSalePrice(new BigDecimal("20000"));product.setPurchasePrice(new BigDecimal("8000"));product.setShade("Tono "+code);return products.saveAndFlush(product);}
 CreateSaleRequest request(UUID key,Long id,int quantity,BigDecimal discount){return new CreateSaleRequest(key,null,PaymentMethod.TRANSFER,SaleChannel.WHATSAPP,discount,"Prueba aislada",List.of(new SaleLineRequest(id,quantity,null)));}
 SaleResponse sell(Product product,int quantity){return service.create(request(UUID.randomUUID(),product.getId(),quantity,BigDecimal.ZERO),"admin-test");}
 int stock(Long id){return products.findById(id).orElseThrow().getStock();}

 @Test void saleStoresHistoricalPricesCostsNamesTonesAndCustomer(){
  var product=product("A",4);var customer=new Customer();customer.setName("Cliente original");customer=customers.save(customer);
  var input=new CreateSaleRequest(UUID.randomUUID(),customer.getId(),PaymentMethod.CASH,SaleChannel.INSTAGRAM,new BigDecimal("1000"),"Nota",List.of(new SaleLineRequest(product.getId(),2,new BigDecimal("18000"))));
  var sale=service.create(input,"administradora");assertEquals(2,stock(product.getId()));assertEquals(0,new BigDecimal("35000").compareTo(sale.total()));assertEquals(0,new BigDecimal("16000").compareTo(sale.costTotal()));assertEquals("administradora",sale.recordedBy());
  product=products.findById(product.getId()).orElseThrow();product.setName("Nombre modificado");product.setPurchasePrice(new BigDecimal("12000"));product.setSalePrice(new BigDecimal("30000"));product.setShade("Otro tono");products.saveAndFlush(product);customer.setName("Cliente modificado");customers.save(customer);
  var history=service.get(sale.id());assertEquals("Cliente original",history.customer());assertEquals("Producto de prueba A",history.items().getFirst().product());assertEquals("Tono A",history.items().getFirst().shade());assertEquals(0,new BigDecimal("8000").compareTo(history.items().getFirst().unitCost()));assertEquals(0,new BigDecimal("19000").compareTo(history.grossProfit()));
 }
 @Test void insufficientStockRollsBackEveryLine(){
  var a=product("A",3);var b=product("B",0);
  var input=new CreateSaleRequest(UUID.randomUUID(),null,PaymentMethod.CASH,SaleChannel.OTHER,BigDecimal.ZERO,null,List.of(new SaleLineRequest(a.getId(),1,null),new SaleLineRequest(b.getId(),1,null)));
  assertThrows(BusinessException.class,()->service.create(input,"admin"));assertEquals(3,stock(a.getId()));assertEquals(0,sales.count());assertEquals(0,movements.count());
 }
 @Test void discountAboveSubtotalAndDuplicateSkusAreRejected(){
  var a=product("A",4);
  assertThrows(BusinessException.class,()->service.create(request(UUID.randomUUID(),a.getId(),1,new BigDecimal("20001")),"admin"));
  var duplicate=new CreateSaleRequest(UUID.randomUUID(),null,PaymentMethod.CASH,SaleChannel.OTHER,BigDecimal.ZERO,null,List.of(new SaleLineRequest(a.getId(),1,null),new SaleLineRequest(a.getId(),1,null)));
  assertThrows(BusinessException.class,()->service.create(duplicate,"admin"));assertEquals(4,stock(a.getId()));assertEquals(0,sales.count());
 }
 @Test void cancellationRestoresStockOnceAndExcludesRevenue(){
  var a=product("A",3);var sale=sell(a,2);var cancellation=service.cancel(sale.id(),"Cliente cancela la compra","admin-cancel");
  assertTrue(cancellation.cancelled());assertEquals("admin-cancel",cancellation.cancelledBy());assertEquals(3,stock(a.getId()));
  service.cancel(sale.id(),"Segundo intento de anulación","otra-admin");assertEquals(3,stock(a.getId()));assertEquals(2,movements.count());assertEquals(0,sales.total(Instant.now().minusSeconds(60),Instant.now().plusSeconds(60)).signum());
 }
 @Test void unknownCostStaysUnknown(){var a=product("A",2);a.setCostConfirmed(false);products.saveAndFlush(a);var sale=sell(a,1);assertNull(sale.costTotal());assertNull(sale.grossProfit());assertNull(sale.items().getFirst().unitCost());}
 @Test void retryReturnsSameSaleAndDifferentPayloadCannotReuseKey(){
  var a=product("A",5);var key=UUID.randomUUID();var input=request(key,a.getId(),2,BigDecimal.ZERO);var first=service.create(input,"admin");var second=service.create(input,"admin");assertEquals(first.id(),second.id());assertEquals(3,stock(a.getId()));assertEquals(1,sales.count());
  assertThrows(BusinessException.class,()->service.create(request(key,a.getId(),1,BigDecimal.ZERO),"admin"));assertEquals(3,stock(a.getId()));
 }
 @Test void concurrentSalesCannotOversellLastUnit()throws Exception{
  var a=product("A",1);var start=new CountDownLatch(1);
  try(var executor=Executors.newFixedThreadPool(2)){
   var tasks=new ArrayList<Future<Boolean>>();for(int i=0;i<2;i++)tasks.add(executor.submit(()->{start.await();try{sell(a,1);return true;}catch(BusinessException expected){return false;}}));start.countDown();int success=0;for(var task:tasks)if(task.get(15,TimeUnit.SECONDS))success++;assertEquals(1,success);
  }
  assertEquals(0,stock(a.getId()));assertEquals(1,sales.count());assertEquals(1,movements.count());
 }
 @Test void concurrentRetryDeductsOnlyOnce()throws Exception{
  var a=product("A",4);var input=request(UUID.randomUUID(),a.getId(),2,BigDecimal.ZERO);var start=new CountDownLatch(1);
  try(var executor=Executors.newFixedThreadPool(2)){
   var first=executor.submit(()->{start.await();return service.create(input,"admin").id();});var second=executor.submit(()->{start.await();return service.create(input,"admin").id();});start.countDown();assertEquals(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS));
  }
  assertEquals(2,stock(a.getId()));assertEquals(1,sales.count());assertEquals(1,movements.count());
 }
 @Test void simultaneousCancellationAndNewSaleKeepCorrectStock()throws Exception{
  var a=product("A",2);var old=sell(a,1);var start=new CountDownLatch(1);
  try(var executor=Executors.newFixedThreadPool(2)){
   var cancellation=executor.submit(()->{start.await();return service.cancel(old.id(),"Cambio de compra","admin");});
   var newSale=executor.submit(()->{start.await();return sell(a,1);});start.countDown();assertTrue(cancellation.get(15,TimeUnit.SECONDS).cancelled());assertFalse(newSale.get(15,TimeUnit.SECONDS).cancelled());
  }
  assertEquals(1,stock(a.getId()));assertEquals(2,sales.count());assertEquals(3,movements.count());
 }
 @Test void historyUsesBogotaDatesAndSupportsChannelStatusAndSearch(){
  var a=product("A",4);var first=sell(a,1);var second=sell(a,1);var third=sell(a,1);
  jdbc.update("update sale set sale_date=? where id=?",Timestamp.from(Instant.parse("2026-10-22T05:00:00Z")),first.id());
  jdbc.update("update sale set sale_date=? where id=?",Timestamp.from(Instant.parse("2026-10-23T04:59:59Z")),second.id());
  jdbc.update("update sale set sale_date=? where id=?",Timestamp.from(Instant.parse("2026-10-23T05:00:00Z")),third.id());
  var day=LocalDate.of(2026,10,22);assertEquals(2,service.list("",day,day,null,null,0,20).getTotalElements());
  service.cancel(first.id(),"Prueba de filtro anulado","admin");assertEquals(1,service.list("A",day,day,false,SaleChannel.WHATSAPP,0,20).getTotalElements());assertEquals(1,service.list(String.valueOf(first.id()),null,null,true,null,0,20).getTotalElements());
  assertThrows(BusinessException.class,()->service.list("",day.plusDays(1),day,null,null,0,20));
 }
}
