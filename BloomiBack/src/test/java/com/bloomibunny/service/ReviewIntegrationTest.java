package com.bloomibunny.service;

import com.bloomibunny.controller.StoreController;
import com.bloomibunny.dto.ProductDtos.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.repository.*;
import com.bloomibunny.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="BLOOMI_TEST_DB_URL",matches="jdbc:postgresql://[^?]+\\?currentSchema=bloomi_sales_test_[a-z0-9_]+")
@SpringBootTest(properties={"spring.datasource.url=${BLOOMI_TEST_DB_URL}","spring.datasource.username=${BLOOMI_TEST_DB_USERNAME}","spring.datasource.password=${BLOOMI_TEST_DB_PASSWORD}","spring.jpa.hibernate.ddl-auto=create-drop","app.bootstrap.admin-username=sales-integration-admin","app.bootstrap.admin-password=integration-fixture-password","jwt.secret=integration-only-signing-key-with-enough-bytes"})
@DirtiesContext
class ReviewIntegrationTest {
 @Autowired ProductService service;
 @Autowired FavoriteService favorites;
 @Autowired ProductRepository products;
 @Autowired CategoryRepository categories;
 @Autowired BrandRepository brands;
 @Autowired SupplierRepository suppliers;
 @Autowired AppUserRepository users;
 @Autowired InventoryMovementRepository movements;
 @Autowired JdbcTemplate jdbc;
 @BeforeEach void reset(){assertTrue(jdbc.queryForObject("select current_schema()",String.class).startsWith("bloomi_sales_test_"));jdbc.execute("truncate table favorite,inventory_movement,sale_item,sale,purchase_item,purchase,expense,product restart identity cascade");}
 Product product(String code,int stock){var p=new Product();p.setName("Producto compartido");p.setCode(code);p.setCategory(categories.findAll().getFirst());p.setStock(stock);p.setSalePrice(new BigDecimal("12000.25"));p.setPurchasePrice(new BigDecimal("5000.50"));return products.saveAndFlush(p);}
 @Test void publicListingHidesUnpublishedProvisionalAndDeletedButKeepsExhausted(){
  var visible=product("VISIBLE",2);var empty=product("EMPTY",0);var hidden=product("HIDDEN",2);hidden.setActive(false);products.saveAndFlush(hidden);var provisional=product("PROVISIONAL",2);provisional.setProvisionalData(true);products.saveAndFlush(provisional);var deleted=product("DELETED",2);deleted.setDeleted(true);products.saveAndFlush(deleted);
  var result=service.publicList("",0,100);assertEquals(2,result.getTotalElements());assertEquals(Set.of(visible.getId(),empty.getId()),new HashSet<>(result.map(ProductResponse::id).getContent()));assertEquals(0,service.publicList("HIDDEN",0,100).getTotalElements());assertEquals(4,service.list("",0,100,"name").getTotalElements());
 }
 @Test void publicDetailsAndToneGroupsExcludeProvisionalReferences(){
  var visible=product("A",2);visible.setVariantGroup("test-group");products.saveAndFlush(visible);var provisional=product("B",2);provisional.setVariantGroup("test-group");provisional.setProvisionalData(true);products.saveAndFlush(provisional);var store=new StoreController(service);assertEquals(1,store.get(visible.getId()).variants().size());assertThrows(ResourceNotFoundException.class,()->store.get(provisional.getId()));
 }
 @Test void paginationIsStableAcrossOneHundredProductsWithIdenticalNames(){
  for(int i=0;i<101;i++)product("P-"+i,1);
  var first=service.publicList("",0,100);var second=service.publicList("",1,100);assertEquals(101,first.getTotalElements());assertEquals(100,first.getContent().size());assertEquals(1,second.getContent().size());var ids=new HashSet<>(first.map(ProductResponse::id).getContent());assertTrue(ids.add(second.getContent().getFirst().id()));assertEquals(101,ids.size());assertEquals(2,service.list("",0,100,"name").getTotalPages());
 }
 @Test void concurrentAdjustmentsDoNotLoseStock(){
  var p=product("A",0);var gate=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->{gate.await();return service.adjust(p.getId(),new StockAdjustment(2,"Entrada uno"));});var b=pool.submit(()->{gate.await();return service.adjust(p.getId(),new StockAdjustment(3,"Entrada dos"));});gate.countDown();a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);}catch(Exception e){throw new RuntimeException(e);}
  assertEquals(5,products.findById(p.getId()).orElseThrow().getStock());assertEquals(2,movements.count());
 }
 @Test void favoritesRemainPrivateIdempotentAndHideProvisionalProducts(){
  var p=product("A",2);String one="review-one-"+UUID.randomUUID(),two="review-two-"+UUID.randomUUID();for(var name:List.of(one,two)){var user=new AppUser();user.setUsername(name);user.setPassword("isolated-fixture");users.saveAndFlush(user);}
  favorites.add(one,p.getId());favorites.add(one,p.getId());assertEquals(1,favorites.list(one).size());assertTrue(favorites.list(two).isEmpty());p.setProvisionalData(true);products.saveAndFlush(p);assertTrue(favorites.list(one).isEmpty());assertThrows(ResourceNotFoundException.class,()->favorites.add(two,p.getId()));
 }
 @Test void editingStoresBrandSupplierDateAndPreservesPrecisePrices(){
  var p=product("A",2);var brand=new Brand();brand.setName("Marca "+UUID.randomUUID());brand=brands.saveAndFlush(brand);var supplier=new Supplier();supplier.setName("Proveedor de revisión");supplier=suppliers.saveAndFlush(supplier);var date=java.time.LocalDate.of(2026,1,15);
  var result=service.update(p.getId(),new ProductRequest(p.getName(),p.getCode(),p.getCategory().getId(),brand.getId(),supplier.getId(),"Detalle","Rosa","3 ml",p.getPurchasePrice(),p.getSalePrice(),2,1,date,true,null,false,true,p.getVersion(),null,null));assertEquals(brand.getId(),result.brandId());assertEquals(supplier.getId(),result.supplierId());assertEquals(date,result.entryDate());assertEquals(0,new BigDecimal("12000.25").compareTo(result.salePrice()));assertEquals(0,movements.count());
 }
}
