package com.bloomibunny.controller;

import com.bloomibunny.service.ProductService;
import com.bloomibunny.dto.ProductDtos.ProductResponse;
import com.bloomibunny.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/store")
public class StoreController {
 private final ProductService products;
 public StoreController(ProductService products){this.products=products;}

 // Commercial data only: internal costs, profit and supplier details stay in the admin API.
 public record StoreProduct(Long id,String name,String category,String brand,String description,String shade,String presentation,BigDecimal salePrice,int stock,boolean active,String imageUrl,String variantGroup,List<StoreProduct> variants,List<String> galleryImages){}

 @GetMapping("/products")
 Page<StoreProduct> list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
  return products.publicList(q,page,size).map(this::publicProduct);
 }

 @GetMapping("/products/{id}")
 public StoreProduct get(@PathVariable Long id){
  var product=products.get(id);
  if(!product.active()||product.provisionalData())throw new ResourceNotFoundException("Producto no disponible");
  var variants=product.variantGroup()==null?List.<StoreProduct>of():products.variants(product.variantGroup()).stream().map(this::publicProduct).toList();
  return new StoreProduct(product.id(),product.name(),product.category(),product.brand(),product.description(),product.shade(),product.presentation(),product.salePrice(),product.stock(),product.active(),product.imageUrl(),product.variantGroup(),variants,product.galleryImages());
 }

 private StoreProduct publicProduct(ProductResponse p){return new StoreProduct(p.id(),p.name(),p.category(),p.brand(),p.description(),p.shade(),p.presentation(),p.salePrice(),p.stock(),p.active(),p.imageUrl(),p.variantGroup(),List.of(),p.galleryImages());}
}
