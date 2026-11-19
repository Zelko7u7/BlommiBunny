package com.bloomibunny.service;

import com.bloomibunny.dto.BannerDtos.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.entity.PromotionalBanner.*;
import com.bloomibunny.exception.*;
import com.bloomibunny.repository.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @Transactional
public class BannerService {
 private final PromotionalBannerRepository banners;private final CategoryRepository categories;private final ProductRepository products;private final CloudinaryImageStorage images;private final JdbcTemplate jdbc;private final Clock clock;
 public BannerService(PromotionalBannerRepository b,CategoryRepository c,ProductRepository p,CloudinaryImageStorage i,JdbcTemplate j,Clock k){banners=b;categories=c;products=p;images=i;jdbc=j;clock=k;}
 private void lock(){jdbc.queryForObject("select 1 from pg_advisory_xact_lock(?)",Integer.class,721604119L);}
 @Transactional(readOnly=true) public List<Response> list(){return banners.findAllByOrderBySortOrderAscIdAsc().stream().map(this::map).toList();}
 @Transactional(readOnly=true) public List<PublicBanner> published(){var now=clock.instant();return banners.findAllByOrderBySortOrderAscIdAsc().stream().filter(b->b.isActive()&&(b.getStartsAt()==null||!now.isBefore(b.getStartsAt()))&&(b.getEndsAt()==null||now.isBefore(b.getEndsAt()))&&available(b)).map(b->new PublicBanner(b.getId(),desktop(b),mobile(b),b.getAltText(),b.getHeadline(),b.getDescription(),href(b),b.getEndsAt())).toList();}
 @Transactional(readOnly=true) public Targets targets(){return new Targets(categories.findAll().stream().filter(Category::isActive).sorted(Comparator.comparing(Category::getName)).map(c->new Option(c.getId(),c.getName())).toList(),products.findAll().stream().filter(this::publicProduct).sorted(Comparator.comparing(Product::getName)).map(p->new Option(p.getId(),p.getName()+(p.getShade()==null?"":" · "+p.getShade()))).toList(),List.of(new SectionOption(PublicSection.ABOUT,"Nosotros"),new SectionOption(PublicSection.FAVORITES,"Favoritos")));}
 public Response create(Request r){lock();var previous=banners.findByRequestKey(r.requestKey());if(previous.isPresent()){if(!same(previous.get(),r))throw new BusinessException("Este intento de creación ya tiene otros datos. Recarga la lista antes de volver a crear.");return map(previous.get());}if(banners.count()>=50)throw new BusinessException("Puedes gestionar hasta 50 banners. Elimina promociones antiguas antes de crear más.");var b=new PromotionalBanner();b.setRequestKey(r.requestKey());apply(b,r);return map(banners.saveAndFlush(b));}
 public Response update(Long id,Request r){lock();var b=find(id);version(b,r.expectedVersion());apply(b,r);return map(banners.saveAndFlush(b));}
 public Response status(Long id,Status r){lock();var b=find(id);version(b,r.expectedVersion());if(r.active()&&!available(b))throw new BusinessException("El destino ya no está disponible. Edita el banner para elegir otro.");b.setActive(r.active());return map(banners.saveAndFlush(b));}
 public void delete(Long id,long version){lock();var b=find(id);version(b,version);banners.delete(b);banners.flush();}
 public List<Response> reorder(Reorder r){lock();var all=banners.findAllByOrderBySortOrderAscIdAsc();var indexed=new HashMap<Long,PromotionalBanner>();all.forEach(b->indexed.put(b.getId(),b));if(r.banners().size()!=all.size()||r.banners().stream().map(Position::id).distinct().count()!=all.size())throw new BannerConflictException();int position=0;for(var item:r.banners()){var b=indexed.get(item.id());if(b==null)throw new BannerConflictException();version(b,item.expectedVersion());b.setSortOrder(position++);}banners.flush();return list();}
 private PromotionalBanner find(Long id){return banners.findById(id).orElseThrow(()->new ResourceNotFoundException("Banner no encontrado"));}
 private void version(PromotionalBanner b,Long expected){if(expected==null||expected!=b.getVersion())throw new BannerConflictException();}
 private boolean publicProduct(Product p){return p!=null&&p.isActive()&&!p.isDeleted()&&!p.isProvisionalData();}
 private boolean available(PromotionalBanner b){return switch(b.getTargetType()){case CATALOG->true;case CATEGORY->b.getCategory()!=null&&b.getCategory().isActive();case PRODUCT->publicProduct(b.getProduct());case SECTION->b.getSection()!=null;};}
 private String href(PromotionalBanner b){if(!available(b))return null;return switch(b.getTargetType()){case CATALOG->"/#catalogo";case CATEGORY->"/?categoryId="+b.getCategory().getId()+"#catalogo";case PRODUCT->"/producto/"+b.getProduct().getId();case SECTION->b.getSection()==PublicSection.ABOUT?"/nosotros":"/favoritos";};}
 private String label(PromotionalBanner b){return switch(b.getTargetType()){case CATALOG->"Catálogo completo";case CATEGORY->b.getCategory()==null?"Categoría eliminada":b.getCategory().getName();case PRODUCT->b.getProduct()==null?"Producto eliminado":b.getProduct().getName()+(b.getProduct().getShade()==null?"":" · "+b.getProduct().getShade());case SECTION->b.getSection()==PublicSection.ABOUT?"Nosotros":"Favoritos";};}
 private String publication(PromotionalBanner b){if(!b.isActive())return "INACTIVE";if(!available(b))return "UNAVAILABLE";var now=clock.instant();if(b.getStartsAt()!=null&&now.isBefore(b.getStartsAt()))return "SCHEDULED";if(b.getEndsAt()!=null&&!now.isBefore(b.getEndsAt()))return "EXPIRED";return "PUBLISHED";}
 private Image desktop(PromotionalBanner b){return new Image(b.getDesktopImageUrl(),b.getDesktopImagePublicId(),b.getDesktopWidth(),b.getDesktopHeight());}
 private Image mobile(PromotionalBanner b){return b.getMobileImageUrl()==null?null:new Image(b.getMobileImageUrl(),b.getMobileImagePublicId(),b.getMobileWidth(),b.getMobileHeight());}
 private Response map(PromotionalBanner b){return new Response(b.getId(),b.getInternalName(),desktop(b),mobile(b),b.getAltText(),b.getHeadline(),b.getDescription(),b.getTargetType(),b.getTargetType()==TargetType.CATEGORY&&b.getCategory()!=null?b.getCategory().getId():b.getTargetType()==TargetType.PRODUCT&&b.getProduct()!=null?b.getProduct().getId():null,b.getSection(),b.getSortOrder(),b.isActive(),b.getStartsAt(),b.getEndsAt(),b.getVersion(),b.getRequestKey(),label(b),available(b),publication(b),href(b));}
 private void image(Image i){if(!images.validBannerImage(i.url(),i.publicId())||(long)i.width()*i.height()>20_000_000)throw new BusinessException("Selecciona una imagen subida desde este formulario de banners.");}
 private void apply(PromotionalBanner b,Request r){
  image(r.desktopImage());if(r.mobileImage()!=null)image(r.mobileImage());
  if(r.startsAt()!=null&&r.endsAt()!=null&&!r.startsAt().isBefore(r.endsAt()))throw new BusinessException("La fecha de finalización debe ser posterior al inicio.");
  if(r.targetType()!=TargetType.SECTION&&r.section()!=null||r.targetType()!=TargetType.CATEGORY&&r.targetType()!=TargetType.PRODUCT&&r.targetReferenceId()!=null)throw new BusinessException("Revisa el destino seleccionado.");
  b.setCategory(null);b.setProduct(null);b.setSection(null);b.setTargetType(r.targetType());
  switch(r.targetType()){
   case CATEGORY->{var c=categories.findById(Objects.requireNonNullElse(r.targetReferenceId(),-1L)).filter(Category::isActive).orElseThrow(()->new BusinessException("Selecciona una categoría activa."));b.setCategory(c);}
   case PRODUCT->{var p=products.findById(Objects.requireNonNullElse(r.targetReferenceId(),-1L)).filter(this::publicProduct).orElseThrow(()->new BusinessException("Selecciona un producto publicado."));b.setProduct(p);}
   case SECTION->{if(r.section()==null)throw new BusinessException("Selecciona una sección pública.");b.setSection(r.section());}
   case CATALOG->{}
  }
  b.setInternalName(r.internalName().trim());b.setAltText(r.altText().trim());b.setHeadline(clean(r.headline()));b.setDescription(clean(r.description()));b.setDesktopImageUrl(r.desktopImage().url());b.setDesktopImagePublicId(r.desktopImage().publicId());b.setDesktopWidth(r.desktopImage().width());b.setDesktopHeight(r.desktopImage().height());
  var m=r.mobileImage();b.setMobileImageUrl(m==null?null:m.url());b.setMobileImagePublicId(m==null?null:m.publicId());b.setMobileWidth(m==null?null:m.width());b.setMobileHeight(m==null?null:m.height());b.setSortOrder(r.sortOrder());b.setActive(r.active());b.setStartsAt(r.startsAt());b.setEndsAt(r.endsAt());
 }
 private String clean(String value){return value==null||value.isBlank()?null:value.trim();}
 private boolean same(PromotionalBanner b,Request r){return b.getInternalName().equals(r.internalName().trim())&&desktop(b).equals(r.desktopImage())&&Objects.equals(mobile(b),r.mobileImage())&&b.getAltText().equals(r.altText().trim())&&Objects.equals(b.getHeadline(),clean(r.headline()))&&Objects.equals(b.getDescription(),clean(r.description()))&&b.getTargetType()==r.targetType()&&Objects.equals(map(b).targetReferenceId(),r.targetReferenceId())&&b.getSection()==r.section()&&b.getSortOrder()==r.sortOrder()&&b.isActive()==r.active()&&Objects.equals(b.getStartsAt(),r.startsAt())&&Objects.equals(b.getEndsAt(),r.endsAt());}
}
