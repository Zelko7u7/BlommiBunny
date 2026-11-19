package com.bloomibunny.service;
import com.bloomibunny.dto.BannerDtos.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.entity.PromotionalBanner.*;
import com.bloomibunny.exception.*;
import com.bloomibunny.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="BLOOMI_TEST_DB_URL",matches="jdbc:postgresql://[^?]+\\?currentSchema=bloomi_sales_test_[a-z0-9_]+")
@SpringBootTest(properties={"spring.datasource.url=${BLOOMI_TEST_DB_URL}","spring.datasource.username=${BLOOMI_TEST_DB_USERNAME}","spring.datasource.password=${BLOOMI_TEST_DB_PASSWORD}","spring.jpa.hibernate.ddl-auto=create-drop","app.bootstrap.admin-username=sales-integration-admin","app.bootstrap.admin-password=integration-fixture-password","jwt.secret=integration-only-signing-key-with-enough-bytes","CLOUDINARY_CLOUD_NAME=banner-test-cloud"})
@DirtiesContext
class BannerIntegrationTest {
 @Autowired BannerService service;@Autowired PromotionalBannerRepository banners;@Autowired CategoryRepository categories;@Autowired ProductRepository products;@Autowired JdbcTemplate jdbc;
 static final Image IMAGE=new Image("https://res.cloudinary.com/banner-test-cloud/image/upload/v1/bloomi-bunny/banners/fixture.webp","bloomi-bunny/banners/fixture",1800,310);
 @BeforeEach void reset(){assertTrue(jdbc.queryForObject("select current_schema()",String.class).startsWith("bloomi_sales_test_"));banners.deleteAll();}
 Request request(UUID key,TargetType type,Long ref,boolean active,Instant start,Instant end,Long version){return new Request(key,"Banner",IMAGE,null,"Promoción accesible",null,null,type,ref,type==TargetType.SECTION?PublicSection.ABOUT:null,0,active,start,end,version);}
 Response create(){return service.create(request(UUID.randomUUID(),TargetType.CATALOG,null,true,null,null,null));}
 Category category(){return categories.findAll().getFirst();}
 Product product(){var p=new Product();p.setCode("BANNER-"+UUID.randomUUID());p.setName("Producto de banner");p.setCategory(category());p.setSalePrice(BigDecimal.TEN);p.setStock(1);return products.saveAndFlush(p);}
 @Test void persistsAndPublishesWithoutChangingProductData(){var p=product();var b=create();assertEquals(1,service.published().size());assertEquals("/#catalogo",service.published().getFirst().href());assertEquals(1,products.findById(p.getId()).orElseThrow().getStock());assertEquals(IMAGE,service.list().getFirst().desktopImage());}
 @Test void createRetryIsIdempotentAndDifferentPayloadIsRejected(){var key=UUID.randomUUID();var r=request(key,TargetType.CATALOG,null,true,null,null,null);var b=service.create(r);assertEquals(b.id(),service.create(r).id());assertThrows(BusinessException.class,()->service.create(request(key,TargetType.CATALOG,null,false,null,null,null)));assertEquals(1,banners.count());}
 @Test void concurrentCreateRetriesDoNotDuplicate()throws Exception{var r=request(UUID.randomUUID(),TargetType.CATALOG,null,true,null,null,null);var gate=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->{gate.await();return service.create(r);});var b=pool.submit(()->{gate.await();return service.create(r);});gate.countDown();assertEquals(a.get(10,TimeUnit.SECONDS).id(),b.get(10,TimeUnit.SECONDS).id());}assertEquals(1,banners.count());}
 @Test void categoryRenameKeepsStableAssociation(){var c=category();var b=service.create(request(UUID.randomUUID(),TargetType.CATEGORY,c.getId(),true,null,null,null));String href=service.published().getFirst().href();c.setName("Categoría renombrada "+UUID.randomUUID());categories.saveAndFlush(c);assertEquals(href,service.published().getFirst().href());assertEquals(c.getName(),service.list().getFirst().targetLabel());assertEquals(c.getId(),b.targetReferenceId());}
 @Test void inactiveCategoryHidesBannerWithoutChangingItsActiveState(){var c=category();service.create(request(UUID.randomUUID(),TargetType.CATEGORY,c.getId(),true,null,null,null));c.setActive(false);categories.saveAndFlush(c);assertTrue(service.published().isEmpty());assertTrue(service.list().getFirst().active());assertFalse(service.list().getFirst().targetAvailable());c.setActive(true);categories.saveAndFlush(c);assertEquals(1,service.published().size());}
 @Test void hiddenDeletedAndProvisionalProductDestinationsAreExcluded(){var p=product();service.create(request(UUID.randomUUID(),TargetType.PRODUCT,p.getId(),true,null,null,null));assertEquals("/producto/"+p.getId(),service.published().getFirst().href());p.setActive(false);p=products.saveAndFlush(p);assertTrue(service.published().isEmpty());p.setActive(true);p.setDeleted(true);p=products.saveAndFlush(p);assertTrue(service.published().isEmpty());p.setDeleted(false);p.setProvisionalData(true);products.saveAndFlush(p);assertTrue(service.published().isEmpty());}
 @Test void targetSelectorsUseOnlyRealAvailableRecords(){var p=product();var c=category();c.setActive(true);categories.saveAndFlush(c);assertTrue(service.targets().products().stream().anyMatch(v->v.id().equals(p.getId())));p.setDeleted(true);products.saveAndFlush(p);assertFalse(service.targets().products().stream().anyMatch(v->v.id().equals(p.getId())));assertEquals(2,service.targets().sections().size());}
 @Test void invalidReferencesAndExternalImagesRollBack(){assertThrows(BusinessException.class,()->service.create(request(UUID.randomUUID(),TargetType.CATEGORY,-1L,true,null,null,null)));var r=new Request(UUID.randomUUID(),"Bad",new Image("https://evil.invalid/image.png","bloomi-bunny/banners/fixture",1800,310),null,"Alt",null,null,TargetType.CATALOG,null,null,0,true,null,null,null);assertThrows(BusinessException.class,()->service.create(r));assertEquals(0,banners.count());}
 @Test void publicationHonorsOptionalDatesAndActiveState(){var now=Instant.now();create();service.create(request(UUID.randomUUID(),TargetType.CATALOG,null,true,now.plusSeconds(3600),null,null));service.create(request(UUID.randomUUID(),TargetType.CATALOG,null,true,null,now.minusSeconds(1),null));service.create(request(UUID.randomUUID(),TargetType.CATALOG,null,false,null,null,null));assertEquals(1,service.published().size());assertEquals(Set.of("PUBLISHED","SCHEDULED","EXPIRED","INACTIVE"),new HashSet<>(service.list().stream().map(Response::publicationStatus).toList()));}
 @Test void reversedDateRangeIsRejected(){assertThrows(BusinessException.class,()->service.create(request(UUID.randomUUID(),TargetType.CATALOG,null,true,Instant.now().plusSeconds(60),Instant.now(),null)));assertEquals(0,banners.count());}
 @Test void staleEditAndStatusDoNotOverwrite(){var b=create();var updated=service.update(b.id(),request(b.requestKey(),TargetType.SECTION,null,true,null,null,b.version()));assertEquals("/nosotros",updated.href());assertThrows(BannerConflictException.class,()->service.status(b.id(),new Status(false,b.version())));assertTrue(service.list().getFirst().active());}
 @Test void allDisabledMeansEmptyAndNoFallback(){var a=create();var b=create();service.status(a.id(),new Status(false,a.version()));service.status(b.id(),new Status(false,b.version()));assertTrue(service.published().isEmpty());assertEquals(2,service.list().size());}
 @Test void deleteRemovesOnlyBannerAndNeverPhotosOrProducts(){var p=product();var b=create();service.delete(b.id(),b.version());assertTrue(service.published().isEmpty());assertTrue(products.findById(p.getId()).isPresent());assertThrows(ResourceNotFoundException.class,()->service.delete(b.id(),b.version()));}
 @Test void reorderIsAtomicAndChecksEntireCurrentList(){var a=create();var b=create();var sorted=service.reorder(new Reorder(List.of(new Position(b.id(),b.version()),new Position(a.id(),a.version()))));assertEquals(List.of(b.id(),a.id()),sorted.stream().map(Response::id).toList());assertThrows(BannerConflictException.class,()->service.reorder(new Reorder(List.of(new Position(a.id(),a.version()),new Position(b.id(),b.version())))));assertEquals(List.of(b.id(),a.id()),service.list().stream().map(Response::id).toList());assertThrows(BannerConflictException.class,()->service.reorder(new Reorder(List.of(new Position(b.id(),sorted.getFirst().version())))));}
 @Test void duplicateReorderIdsCannotHideOrDuplicateABanner(){var a=create();create();assertThrows(BannerConflictException.class,()->service.reorder(new Reorder(List.of(new Position(a.id(),a.version()),new Position(a.id(),a.version())))));assertEquals(2,banners.count());}
}
