package com.bloomibunny.service;

import com.bloomibunny.dto.DashboardResponse;
import com.bloomibunny.dto.SaleDtos.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.repository.*;
import com.bloomibunny.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="BLOOMI_TEST_DB_URL",matches="jdbc:postgresql://[^?]+\\?currentSchema=bloomi_sales_test_[a-z0-9_]+")
@SpringBootTest(properties={"spring.datasource.url=${BLOOMI_TEST_DB_URL}","spring.datasource.username=${BLOOMI_TEST_DB_USERNAME}","spring.datasource.password=${BLOOMI_TEST_DB_PASSWORD}","spring.jpa.hibernate.ddl-auto=create-drop","app.bootstrap.admin-username=sales-integration-admin","app.bootstrap.admin-password=integration-fixture-password","jwt.secret=integration-only-signing-key-with-enough-bytes"})
@DirtiesContext
class DashboardIntegrationTest {
 @Autowired AdminSaleService sales;
 @Autowired ProductService inventory;
 @Autowired DashboardRepository data;
 @Autowired ProductRepository products;
 @Autowired CategoryRepository categories;
 @Autowired CustomerRepository customers;
 @Autowired InventoryMovementRepository movements;
 @Autowired PlatformTransactionManager transactions;
 @Autowired JdbcTemplate jdbc;
 @BeforeEach void reset(){assertTrue(jdbc.queryForObject("select current_schema()",String.class).startsWith("bloomi_sales_test_"));jdbc.execute("truncate table favorite,inventory_movement,sale_item,sale,product restart identity cascade");}
 DashboardResponse dashboard(){return new TransactionTemplate(transactions).execute(status->new DashboardServiceImpl(data,products,customers,movements,Clock.fixed(Instant.parse("2026-11-01T02:30:00Z"),ZoneOffset.UTC)).summary());}
 Product product(String code,int stock,String price,String cost){var p=new Product();p.setName("Producto "+code);p.setCode(code);p.setShade("Tono "+code);p.setStock(stock);p.setSalePrice(new BigDecimal(price));p.setPurchasePrice(new BigDecimal(cost));p.setCategory(categories.findAll().getFirst());return products.saveAndFlush(p);}
 SaleResponse sell(Product product,int quantity,String discount,String instant){var sale=sales.create(new CreateSaleRequest(UUID.randomUUID(),null,PaymentMethod.CASH,SaleChannel.WHATSAPP,new BigDecimal(discount),null,List.of(new SaleLineRequest(product.getId(),quantity,null))),"dashboard-test");jdbc.update("update sale set sale_date=? where id=?",Timestamp.from(Instant.parse(instant)),sale.id());return sale;}
 @Test void revenueCostsAndRankingExcludeCancellationsAndKeepHistoricalSnapshots(){
  var a=product("A",10,"20000","8000");var b=product("B",10,"10000","3000");
  sell(a,2,"1000","2026-10-31T05:00:00Z");sell(b,1,"0","2026-10-31T04:59:59Z");var cancelled=sell(a,1,"0","2026-10-31T06:00:00Z");sales.cancel(cancelled.id(),"Prueba anulada","admin");
  a=products.findById(a.getId()).orElseThrow();a.setName("Nombre actual distinto");a.setPurchasePrice(new BigDecimal("18000"));products.saveAndFlush(a);
  var result=dashboard();assertEquals(0,new BigDecimal("39000").compareTo(result.salesToday()));assertEquals(1,result.salesTodayCount());assertEquals(0,new BigDecimal("49000").compareTo(result.salesMonth()));assertEquals(2,result.salesMonthCount());assertEquals(0,new BigDecimal("19000").compareTo(result.costMonth()));assertEquals(0,new BigDecimal("30000").compareTo(result.grossProfit()));assertEquals(2,result.topProducts().getFirst().units());assertEquals("Producto A",result.topProducts().getFirst().product());assertEquals("Tono A",result.topProducts().getFirst().shade());assertEquals(0,new BigDecimal("39000").compareTo(result.dailySales().getLast().total()));
 }
 @Test void unknownCostsRemainPendingAndCancelledUnknownCostNoLongerBlocksMargin(){
  var a=product("A",3,"20000","8000");a.setCostConfirmed(false);products.saveAndFlush(a);var b=product("B",2,"10000","0");
  var unknown=sell(a,2,"0","2026-10-31T12:00:00Z");sell(b,1,"0","2026-10-31T13:00:00Z");var result=dashboard();assertNull(result.costMonth());assertNull(result.grossProfit());assertEquals(1,result.unknownCostSales());assertEquals(2,result.unknownCostUnits());assertEquals(0,result.knownCostMonth().signum());sales.cancel(unknown.id(),"Se cancela prueba","admin");result=dashboard();assertEquals(0,result.unknownCostSales());assertEquals(0,result.costMonth().signum());assertEquals(0,new BigDecimal("10000").compareTo(result.grossProfit()));
 }
 @Test void graphsUseLocalDayAndMonthBoundariesWithZeroesForMissingPeriods(){
  var a=product("A",10,"20000","8000");sell(a,1,"0","2026-04-01T05:00:00Z");sell(a,1,"0","2026-04-01T04:59:59Z");sell(a,1,"0","2026-11-01T04:59:59Z");sell(a,1,"0","2026-11-01T05:00:00Z");
  var result=dashboard();assertEquals(LocalDate.of(2026,10,31),result.today());assertEquals(1,result.salesTodayCount());assertEquals(1,result.salesMonthCount());assertEquals(7,result.monthlySales().size());assertEquals(1,result.monthlySales().getFirst().count());assertEquals(LocalDate.of(2026,4,1),result.monthlySales().getFirst().date());assertEquals(0,result.monthlySales().get(1).count());assertEquals(1,result.monthlySales().getLast().count());assertEquals(30,result.dailySales().size());assertEquals(1,result.dailySales().getLast().count());assertEquals(1,result.topProducts().getFirst().units());
 }
 @Test void inventoryAlertsAndDestinationFiltersAgreeIncludingHiddenButNotDeletedSkus(){
  var zero=product("ZERO",0,"1000","500");zero.setActive(false);products.saveAndFlush(zero);
  var low=product("LOW",2,"1000","500");low.setMinimumStock(2);products.saveAndFlush(low);
  var hidden=product("HIDDEN",1,"1000","500");hidden.setMinimumStock(3);hidden.setActive(false);products.saveAndFlush(hidden);
  product("NORMAL",10,"1000","500");var deleted=product("DELETED",0,"1000","500");deleted.setDeleted(true);deleted.setActive(false);products.saveAndFlush(deleted);
  var result=dashboard();assertEquals(2,result.lowStock());assertEquals(1,result.outOfStock());assertEquals(4,result.totalProducts());assertEquals(2,result.activeProducts());assertEquals(3,result.stockAlerts().size());assertEquals(zero.getId(),result.stockAlerts().getFirst().id());
  assertEquals(2,inventory.listByStock("",0,100,"name","low").getTotalElements());assertEquals(1,inventory.listByStock("",0,100,"name","out").getTotalElements());assertEquals(3,inventory.summaryByStock("","low").confirmedUnits());assertEquals(1,inventory.summaryByStock("LOW","low").totalProducts());assertEquals(0,inventory.summaryByStock("","out").confirmedUnits());
 }
 @Test void creatingThenCancellingSaleChangesDashboardAndRestoresInventory(){
  var a=product("A",5,"20000","8000");assertEquals(0,dashboard().salesMonth().signum());var sale=sell(a,3,"500","2026-10-31T12:00:00Z");var result=dashboard();assertEquals(0,new BigDecimal("59500").compareTo(result.salesMonth()));assertEquals(0,new BigDecimal("35500").compareTo(result.grossProfit()));assertEquals(3,result.topProducts().getFirst().units());sales.cancel(sale.id(),"Compra anulada","admin");result=dashboard();assertEquals(0,result.salesMonth().signum());assertEquals(0,result.costMonth().signum());assertTrue(result.topProducts().isEmpty());assertEquals(5,products.findById(a.getId()).orElseThrow().getStock());assertEquals(2,result.recentMovements().size());
 }
}
