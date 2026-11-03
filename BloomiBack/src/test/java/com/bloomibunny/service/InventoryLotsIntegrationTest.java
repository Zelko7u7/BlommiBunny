package com.bloomibunny.service;

import com.bloomibunny.dto.SaleDtos.*;
import com.bloomibunny.dto.TransactionDtos.*;
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
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="BLOOMI_TEST_DB_URL",matches="jdbc:postgresql://[^?]+\\?currentSchema=bloomi_sales_test_[a-z0-9_]+")
@SpringBootTest(properties={"spring.datasource.url=${BLOOMI_TEST_DB_URL}","spring.datasource.username=${BLOOMI_TEST_DB_USERNAME}","spring.datasource.password=${BLOOMI_TEST_DB_PASSWORD}","spring.jpa.hibernate.ddl-auto=create-drop","app.bootstrap.admin-username=sales-integration-admin","app.bootstrap.admin-password=integration-fixture-password","jwt.secret=integration-only-signing-key-with-enough-bytes"})
@DirtiesContext
class InventoryLotsIntegrationTest {
 @Autowired AdminPurchaseService purchases;
 @Autowired AdminSaleService sales;
 @Autowired InventoryLotService lots;
 @Autowired ProductService productService;
 @Autowired ProductRepository products;
 @Autowired InventoryLotRepository lotRows;
 @Autowired CategoryRepository categories;
 @Autowired SupplierRepository suppliers;
 @Autowired DashboardRepository dashboard;
 @Autowired JdbcTemplate jdbc;
 Supplier supplier;
 @BeforeEach void reset(){assertTrue(jdbc.queryForObject("select current_schema()",String.class).startsWith("bloomi_sales_test_"));jdbc.execute("truncate table favorite,inventory_movement,sale_item,sale,purchase_item,purchase,expense,product,supplier restart identity cascade");supplier=new Supplier();supplier.setName("Proveedor de prueba");supplier=suppliers.saveAndFlush(supplier);}
 Product product(int stock,String cost){var p=new Product();p.setCode("LOT-TEST");p.setName("Gloss por lotes");p.setCategory(categories.findAll().getFirst());p.setStock(stock);p.setSalePrice(new BigDecimal("30000"));p.setPurchasePrice(new BigDecimal(cost));return products.saveAndFlush(p);}
 PurchaseResponse receive(Product p,int qty,String cost){return receive(p,qty,cost,LocalDate.of(2026,1,15));}
 PurchaseResponse receive(Product p,int qty,String cost,LocalDate date){return purchases.create(new PurchaseRequest(UUID.randomUUID(),supplier.getId(),date,null,null,List.of(new LineRequest(p.getId(),qty,new BigDecimal(cost)))),"admin");}
 SaleResponse sell(Product p,int qty){return sales.create(new CreateSaleRequest(UUID.randomUUID(),null,PaymentMethod.CASH,SaleChannel.OTHER,BigDecimal.ZERO,null,List.of(new SaleLineRequest(p.getId(),qty,null))),"admin");}
 void amount(String expected,BigDecimal actual){assertNotNull(actual);assertEquals(0,new BigDecimal(expected).compareTo(actual));}
 @Test void restockKeepsOneSkuAndValuesEachReceipt(){var p=product(3,"10000");receive(p,5,"12000");assertEquals(1,products.count());assertEquals(8,products.findById(p.getId()).orElseThrow().getStock());assertEquals(2,lots.list(p).size());amount("90000",productService.summary("").confirmedCostValue());amount("10000",products.findById(p.getId()).orElseThrow().getPurchasePrice());}
 @Test void fifoDeletesExhaustedLotButRetainsHistoricalCosts(){var p=product(3,"10000");receive(p,5,"12000");var sale=sell(p,4);amount("42000",sale.costTotal());assertEquals(2,sale.items().getFirst().costAllocations().size());assertEquals(1,lots.list(p).size());assertEquals(4,lots.list(p).getFirst().remaining());amount("48000",productService.summary("").confirmedCostValue());amount("42000",sales.get(sale.id()).costTotal());}
 @Test void lastLotRemainsAtZeroUntilNextRestock(){var p=product(1,"10");var sale=sell(p,1);assertEquals(1,lots.list(p).size());assertEquals(0,lots.list(p).getFirst().remaining());receive(p,2,"12");assertEquals(1,lots.list(p).size());assertEquals(2,lots.list(p).getFirst().remaining());assertFalse(lots.list(p).getFirst().opening());amount("10",sales.get(sale.id()).costTotal());}
 @Test void cancellationRecreatesDeletedLotsWithTheirOriginalCostAndPosition(){var p=product(3,"10000");receive(p,5,"12000");var sale=sell(p,4);sales.cancel(sale.id(),"Devolución completa","admin");assertEquals(2,lots.list(p).size());assertTrue(lots.list(p).getFirst().opening());assertEquals(3,lots.list(p).getFirst().remaining());amount("90000",productService.summary("").confirmedCostValue());sales.cancel(sale.id(),"Devolución repetida","admin");assertEquals(8,products.findById(p.getId()).orElseThrow().getStock());}
 @Test void laterRestockCannotReplaceConsumedPurchaseOnCancellation(){var p=product(0,"1");var receipt=receive(p,2,"10");sell(p,1);receive(p,4,"20");assertThrows(BusinessException.class,()->purchases.cancel(receipt.id(),"Compra equivocada","admin"));assertEquals(5,products.findById(p.getId()).orElseThrow().getStock());}
 @Test void cancellingUnconsumedReceiptPreservesOtherLots(){var p=product(3,"10");var receipt=receive(p,2,"12");purchases.cancel(receipt.id(),"Compra duplicada","admin");assertEquals(1,lots.list(p).size());amount("30",productService.summary("").confirmedCostValue());}
 @Test void cancellingOnlyReceiptKeepsOneEmptyLot(){var p=product(0,"1");var receipt=receive(p,2,"10");purchases.cancel(receipt.id(),"Compra duplicada","admin");assertEquals(1,lots.list(p).size());assertEquals(0,lots.list(p).getFirst().remaining());}
 @Test void mixedCostsKeepExactTotalInsteadOfRoundedAverage(){var p=product(1,"0.01");receive(p,2,"0.02");var sale=sell(p,3);amount("0.05",sale.costTotal());amount("0.05",sale.items().getFirst().costTotal());var now=Instant.now();amount("0.05",dashboard.costs(now.minusSeconds(60),now.plusSeconds(60)).getKnownCost());}
 @Test void unconfirmedOpeningCostIsNotInventedByNewReceipt(){var p=product(2,"10");p.setCostConfirmed(false);products.saveAndFlush(p);receive(p,3,"12");var summary=productService.summary("");assertEquals(2,summary.unknownCostUnits());amount("36",summary.confirmedCostValue());assertNull(sell(p,3).costTotal());}
 @Test void stockAdjustmentsConsumeFifoAndCreateSeparateEntries(){var p=product(3,"10");receive(p,3,"12");productService.adjust(p.getId(),new com.bloomibunny.dto.ProductDtos.StockAdjustment(-4,"Ajuste verificado"));assertEquals(1,lots.list(p).size());amount("24",productService.summary("").confirmedCostValue());productService.adjust(p.getId(),new com.bloomibunny.dto.ProductDtos.StockAdjustment(2,"Entrada verificada"));assertEquals(2,lots.list(p).size());amount("44",productService.summary("").confirmedCostValue());}
 @Test void receiptDatesDetermineFifoWithinNewPurchases(){var p=product(0,"1");receive(p,2,"20",LocalDate.of(2026,1,20));receive(p,2,"10",LocalDate.of(2026,1,10));amount("10",sell(p,1).costTotal());}
 @Test void retryNeverDuplicatesLotsOrCostAllocations(){var p=product(0,"1");var request=new PurchaseRequest(UUID.randomUUID(),supplier.getId(),LocalDate.of(2026,1,15),null,null,List.of(new LineRequest(p.getId(),3,new BigDecimal("10"))));purchases.create(request,"admin");purchases.create(request,"admin");assertEquals(1,lots.list(p).size());var saleRequest=new CreateSaleRequest(UUID.randomUUID(),null,PaymentMethod.CASH,SaleChannel.OTHER,BigDecimal.ZERO,null,List.of(new SaleLineRequest(p.getId(),2,null)));var a=sales.create(saleRequest,"admin");assertEquals(a.id(),sales.create(saleRequest,"admin").id());assertEquals(1,lots.list(p).getFirst().remaining());assertEquals(1,sales.get(a.id()).items().getFirst().costAllocations().size());}
 @Test void editingReferenceCostDoesNotRevalueExistingLots(){
  var p=product(3,"10");receive(p,2,"12");var current=productService.get(p.getId());
  productService.update(p.getId(),new com.bloomibunny.dto.ProductDtos.ProductRequest(current.name(),current.code(),current.categoryId(),current.brandId(),current.supplierId(),current.description(),current.shade(),current.presentation(),new BigDecimal("99"),current.salePrice(),current.stock(),current.minimumStock(),current.entryDate(),current.active(),current.imageUrl(),current.provisionalData(),current.costConfirmed(),current.version(),current.variantGroup(),current.galleryImages()));
  amount("54",productService.summary("").confirmedCostValue());amount("10",sell(p,1).costTotal());
 }
}
