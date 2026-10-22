package com.bloomibunny.service;

import com.bloomibunny.dto.ProductDtos.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.exception.*;
import com.bloomibunny.repository.*;
import com.bloomibunny.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductLifecycleTest {
 @Mock ProductRepository products;
 @Mock CategoryRepository categories;
 @Mock BrandRepository brands;
 @Mock SupplierRepository suppliers;
 @Mock InventoryMovementRepository movements;
 @InjectMocks ProductServiceImpl service;

 Product product(){var c=new Category();c.setName("Labios");var p=new Product();p.setName("Gloss");p.setCode("TEST-GLOSS");p.setCategory(c);p.setStock(3);p.setPurchasePrice(new BigDecimal("5000"));p.setSalePrice(new BigDecimal("12000"));p.setImageUrl("/products/gloss.png");return p;}
 ProductRequest request(){return new ProductRequest("Gloss","TEST-GLOSS",1L,null,null,null,null,null,new BigDecimal("5000"),new BigDecimal("12000"),3,1,null,true,"/products/gloss.png",false,true,null,null);}

 @Test void disablingAndEnablingChangesOnlyPublication(){
  var p=product();when(products.findById(1L)).thenReturn(Optional.of(p));when(products.saveAndFlush(p)).thenReturn(p);
  assertFalse(service.setActive(1L,false).active());assertTrue(service.setActive(1L,true).active());
  assertEquals(3,p.getStock());assertEquals(new BigDecimal("12000"),p.getSalePrice());assertFalse(p.isDeleted());
  verifyNoInteractions(movements);
 }

 @Test void deletionKeepsRecordAndHistoryButBlocksReactivation(){
  var p=product();when(products.findById(1L)).thenReturn(Optional.of(p));
  service.delete(1L);assertTrue(p.isDeleted());assertFalse(p.isActive());assertEquals(3,p.getStock());
  verify(products).save(p);verify(products,never()).delete(any());verifyNoInteractions(movements);
  assertThrows(ResourceNotFoundException.class,()->service.setActive(1L,true));
 }

 @Test void inventoryListingUsesNonDeletedProducts(){
  when(products.findByDeletedFalse(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(product())));
  assertEquals(1,service.list("",0,20,"name").getTotalElements());
  verify(products,never()).findAll(any(Pageable.class));
 }

 @Test void creationRecordsOpeningStockAndConfirmedProfit(){
  var category=new Category();category.setName("Labios");when(categories.findById(1L)).thenReturn(Optional.of(category));
  when(products.save(any())).thenAnswer(call->{Product p=call.getArgument(0);ReflectionTestUtils.setField(p,"id",10L);return p;});
  var response=service.create(request());assertEquals(10L,response.id());assertEquals(3,response.stock());assertEquals(new BigDecimal("7000"),response.profit());
  ArgumentCaptor<InventoryMovement> movement=ArgumentCaptor.forClass(InventoryMovement.class);verify(movements).save(movement.capture());
  assertEquals(3,movement.getValue().getQuantity());assertEquals(3,movement.getValue().getResultingStock());
 }

 @Test void duplicateCodeDoesNotCreateAnything(){
  when(products.existsByCode("TEST-GLOSS")).thenReturn(true);
  assertThrows(BusinessException.class,()->service.create(request()));verify(products,never()).save(any());verifyNoInteractions(categories,movements);
 }

 ProductRequest edit(int stock,BigDecimal price,long expectedVersion){return new ProductRequest("Gloss editado","TEST-GLOSS",1L,null,null,"Descripción actualizada","Rosa","3 ml",new BigDecimal("5000"),price,stock,1,null,true,"/products/gloss.png",false,true,expectedVersion,null);}

 @Test void editingStockRecordsOnlyTheDifference(){
  var p=product();var category=new Category();category.setName("Labios");
  when(products.findById(1L)).thenReturn(Optional.of(p));when(categories.findById(1L)).thenReturn(Optional.of(category));when(products.saveAndFlush(p)).thenReturn(p);
  var response=service.update(1L,edit(5,new BigDecimal("18000"),0));
  assertEquals(5,response.stock());assertEquals(new BigDecimal("18000"),response.salePrice());assertEquals("Descripción actualizada",response.description());
  ArgumentCaptor<InventoryMovement> movement=ArgumentCaptor.forClass(InventoryMovement.class);verify(movements).save(movement.capture());
  assertEquals(2,movement.getValue().getQuantity());assertEquals(5,movement.getValue().getResultingStock());assertEquals(MovementType.ADJUSTMENT_IN,movement.getValue().getType());
 }

 @Test void priceAndDescriptionEditDoesNotInventStockMovement(){
  var p=product();var category=new Category();category.setName("Labios");
  when(products.findById(1L)).thenReturn(Optional.of(p));when(categories.findById(1L)).thenReturn(Optional.of(category));when(products.saveAndFlush(p)).thenReturn(p);
  var response=service.update(1L,edit(3,new BigDecimal("18000"),0));
  assertEquals(new BigDecimal("13000"),response.profit());assertEquals("Rosa",response.shade());verifyNoInteractions(movements);
 }

 @Test void staleEditCannotOverwriteNewerStockOrPrices(){
  var p=product();ReflectionTestUtils.setField(p,"version",1L);when(products.findById(1L)).thenReturn(Optional.of(p));
  assertThrows(org.springframework.orm.ObjectOptimisticLockingFailureException.class,()->service.update(1L,edit(9,new BigDecimal("18000"),0)));
  assertEquals(3,p.getStock());assertEquals(new BigDecimal("12000"),p.getSalePrice());verify(products,never()).saveAndFlush(any());verifyNoInteractions(categories,movements);
 }

 @Test void editingCannotReuseAnotherProductCode(){
  var p=product();when(products.findById(1L)).thenReturn(Optional.of(p));when(products.existsByCodeAndIdNot("TEST-GLOSS",1L)).thenReturn(true);
  assertThrows(BusinessException.class,()->service.update(1L,edit(3,new BigDecimal("18000"),0)));verifyNoInteractions(categories,movements);
 }

 @Test void valuationIncludesConfirmedKittyAndSkullWithoutInventingPurchaseCosts(){
  var kitty=product();kitty.setSalePrice(new BigDecimal("30000"));kitty.setStock(6);kitty.setCostConfirmed(false);
  var skull=product();skull.setSalePrice(new BigDecimal("40000"));skull.setStock(7);skull.setCostConfirmed(false);
  when(products.findByDeletedFalse(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(product(),kitty,skull)));
  var summary=service.summary("");
  assertEquals(16,summary.confirmedUnits());assertEquals(new BigDecimal("496000"),summary.saleValue());
  assertEquals(new BigDecimal("15000"),summary.confirmedCostValue());
  assertEquals(2,summary.unknownCostProducts());assertEquals(13,summary.unknownCostUnits());
 }

 @Test void valuationUsesNewPricesAndStockImmediatelyAfterEdit(){
  var p=product();var category=new Category();category.setName("Labios");
  when(products.findByDeletedFalse(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(p)));
  when(products.findById(1L)).thenReturn(Optional.of(p));when(categories.findById(1L)).thenReturn(Optional.of(category));when(products.saveAndFlush(p)).thenReturn(p);
  assertEquals(new BigDecimal("36000"),service.summary("").saleValue());
  service.update(1L,edit(5,new BigDecimal("18000"),0));
  assertEquals(new BigDecimal("90000"),service.summary("").saleValue());
  assertEquals(new BigDecimal("25000"),service.summary("").confirmedCostValue());
 }

 @Test void valuationUsesAllMatchingProductsAndExcludesProvisionalValues(){
  var inventory=new ArrayList<Product>();
  for(int i=0;i<101;i++){var p=product();p.setSalePrice(new BigDecimal("12000.25"));p.setActive(false);inventory.add(p);}
  var provisional=product();provisional.setProvisionalData(true);inventory.add(provisional);
  when(products.search("Gloss",Pageable.unpaged())).thenReturn(new PageImpl<>(inventory));
  var summary=service.summary("Gloss");
  assertEquals(102,summary.totalProducts());assertEquals(303,summary.confirmedUnits());
  assertEquals(new BigDecimal("3636075.75"),summary.saleValue());
  assertEquals(1,summary.provisionalProducts());assertEquals(3,summary.provisionalUnits());
  verify(products).search("Gloss",Pageable.unpaged());
 }

 @Test void emptyInventoryHasZeroValuation(){
  when(products.findByDeletedFalse(Pageable.unpaged())).thenReturn(Page.empty());
  var summary=service.summary("");assertEquals(0,summary.confirmedUnits());assertEquals(BigDecimal.ZERO,summary.saleValue());assertEquals(0,summary.unknownCostUnits());
 }

 @Test void variantsAreLoadedFromOnlyThePublishedNonDeletedGroup(){
  var p=product();p.setVariantGroup("toy-story-labial");p.setShade("Tono 3");
  when(products.findByVariantGroupAndActiveTrueAndDeletedFalseOrderByShadeAsc("toy-story-labial")).thenReturn(List.of(p));
  var variants=service.variants("toy-story-labial");assertEquals(1,variants.size());assertEquals("Tono 3",variants.getFirst().shade());assertEquals("toy-story-labial",variants.getFirst().variantGroup());
  verify(products,never()).findAll();
 }
}
