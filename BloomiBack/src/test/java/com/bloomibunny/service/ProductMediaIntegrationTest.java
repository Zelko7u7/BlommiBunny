package com.bloomibunny.service;

import com.bloomibunny.controller.StoreController;
import com.bloomibunny.dto.ProductDtos.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="BLOOMI_TEST_DB_URL",matches="jdbc:postgresql://[^?]+\\?currentSchema=bloomi_sales_test_[a-z0-9_]+")
@SpringBootTest(properties={"spring.datasource.url=${BLOOMI_TEST_DB_URL}","spring.datasource.username=${BLOOMI_TEST_DB_USERNAME}","spring.datasource.password=${BLOOMI_TEST_DB_PASSWORD}","spring.jpa.hibernate.ddl-auto=create-drop","app.bootstrap.admin-username=sales-integration-admin","app.bootstrap.admin-password=integration-fixture-password","jwt.secret=integration-only-signing-key-with-enough-bytes"})
@DirtiesContext
class ProductMediaIntegrationTest {
 @Autowired ProductService service;
 @Autowired ProductRepository products;
 @Autowired CategoryRepository categories;
 @Autowired InventoryMovementRepository movements;
 @Autowired JdbcTemplate jdbc;
 @BeforeEach void reset(){assertTrue(jdbc.queryForObject("select current_schema()",String.class).startsWith("bloomi_sales_test_"));jdbc.execute("truncate table favorite,inventory_movement,sale_item,sale,purchase_item,purchase,expense,product restart identity cascade");}
 Product product(String code,String shade){var p=new Product();p.setName("Labial de prueba");p.setCode(code);p.setCategory(categories.findAll().getFirst());p.setStock(4);p.setPurchasePrice(new BigDecimal("5000.50"));p.setSalePrice(new BigDecimal("15000.25"));p.setImageUrl("/products/"+code+".webp");p.setShade(shade);p.setVariantGroup("labial-prueba");p.setDescription("Primer párrafo con ácidos y acentos.\n\nSegundo párrafo, íntegro.");p.getGalleryImages().addAll(List.of("/products/"+code+"-2.webp","/products/"+code+"-3.avif"));return products.saveAndFlush(p);}
 ProductRequest request(ProductResponse p,List<String> gallery){return new ProductRequest(p.name(),p.code(),p.categoryId(),p.brandId(),p.supplierId(),p.description(),p.shade(),p.presentation(),p.purchasePrice(),p.salePrice(),p.stock(),p.minimumStock(),p.entryDate(),p.active(),p.imageUrl(),p.provisionalData(),p.costConfirmed(),p.version(),p.variantGroup(),gallery);}
 @Test void orderedGalleryAndCompleteParagraphsPersistWithoutChangingCommerce(){var p=product("A","Tono 2");var current=service.get(p.getId());var gallery=List.of("/products/new-3.avif","/products/new-2.webp");var saved=service.update(p.getId(),request(current,gallery));assertEquals(gallery,service.get(p.getId()).galleryImages());assertEquals(current.description(),saved.description());assertEquals(current.imageUrl(),saved.imageUrl());assertEquals(current.stock(),saved.stock());assertEquals(current.purchasePrice(),saved.purchasePrice());assertEquals(current.salePrice(),saved.salePrice());assertEquals(current.variantGroup(),saved.variantGroup());assertEquals(0,movements.count());}
 @Test void previousClientsDoNotEraseImagesWhenTheyOmitGallery(){var p=product("A","Tono 2");var current=service.get(p.getId());service.update(p.getId(),request(current,null));assertEquals(current.galleryImages(),service.get(p.getId()).galleryImages());}
 @Test void replacingGalleryAdvancesVersionAndRejectsStaleEdits(){var p=product("A","Tono 2");var old=service.get(p.getId());var saved=service.update(p.getId(),request(old,List.of("/products/new.webp")));assertTrue(saved.version()>old.version());assertThrows(org.springframework.dao.OptimisticLockingFailureException.class,()->service.update(p.getId(),request(old,List.of())));assertEquals(List.of("/products/new.webp"),service.get(p.getId()).galleryImages());}
 @Test void explicitEmptyListRemovesOnlySecondaryImages(){var p=product("A","Tono 2");var current=service.get(p.getId());service.update(p.getId(),request(current,List.of()));var saved=service.get(p.getId());assertTrue(saved.galleryImages().isEmpty());assertEquals(current.imageUrl(),saved.imageUrl());assertEquals(current.stock(),saved.stock());assertEquals(0,movements.count());}
 @Test void eachToneReturnsOnlyItsOwnPhotosAndHiddenVariantsStayHidden(){var a=product("A","Tono 2");var b=product("B","Tono 3");var hidden=product("HIDDEN","Tono 4");hidden.setActive(false);products.saveAndFlush(hidden);var store=new StoreController(service);var detail=store.get(a.getId());assertEquals(2,detail.variants().size());assertEquals(List.of("/products/A-2.webp","/products/A-3.avif"),detail.galleryImages());var tone=detail.variants().stream().filter(v->v.id().equals(b.getId())).findFirst().orElseThrow();assertEquals(List.of("/products/B-2.webp","/products/B-3.avif"),tone.galleryImages());assertFalse(tone.galleryImages().contains(detail.galleryImages().getFirst()));}
 @Test void unsafeGalleryRollsBackEntireProductUpdate(){var p=product("A","Tono 2");var current=service.get(p.getId());assertThrows(com.bloomibunny.exception.BusinessException.class,()->service.update(p.getId(),request(current,List.of("javascript:alert(1)"))));assertEquals(current,service.get(p.getId()));assertEquals(0,movements.count());}
}
