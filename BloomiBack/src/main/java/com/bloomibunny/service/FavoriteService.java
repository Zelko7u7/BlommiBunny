package com.bloomibunny.service;

import com.bloomibunny.controller.StoreController.StoreProduct;
import com.bloomibunny.repository.*;
import com.bloomibunny.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @Transactional
public class FavoriteService {
 private final FavoriteRepository favorites;
 private final AppUserRepository users;
 private final ProductRepository products;
 public FavoriteService(FavoriteRepository favorites,AppUserRepository users,ProductRepository products){this.favorites=favorites;this.users=users;this.products=products;}

 @Transactional(readOnly=true)
 public List<StoreProduct> list(String username){
  return favorites.visible(username).stream().map(f->{var p=f.getProduct();return new StoreProduct(p.getId(),p.getName(),p.getCategory().getName(),p.getBrand()==null?null:p.getBrand().getName(),p.getDescription(),p.getShade(),p.getPresentation(),p.getSalePrice(),p.getStock(),p.isActive(),p.getImageUrl(),p.getVariantGroup(),List.of());}).toList();
 }
 public void add(String username,Long productId){
  var user=users.findByUsername(username).filter(u->u.isActive()).orElseThrow(()->new ResourceNotFoundException("Cuenta no disponible"));
  var product=products.findById(productId).filter(p->p.isActive()&&!p.isDeleted()).orElseThrow(()->new ResourceNotFoundException("Producto no disponible"));
  favorites.add(user.getId(),product.getId());
 }
 public void remove(String username,Long productId){favorites.remove(username,productId);}
}
