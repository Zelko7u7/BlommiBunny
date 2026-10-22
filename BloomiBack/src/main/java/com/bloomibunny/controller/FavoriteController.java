package com.bloomibunny.controller;

import com.bloomibunny.service.FavoriteService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.security.Principal;
import java.util.List;

@RestController @RequestMapping("/api/favorites")
public class FavoriteController {
 private final FavoriteService favorites;
 public FavoriteController(FavoriteService favorites){this.favorites=favorites;}
 @GetMapping public List<StoreController.StoreProduct> list(Principal user){return favorites.list(user.getName());}
 @PutMapping("/{productId}") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void add(Principal user,@PathVariable Long productId){favorites.add(user.getName(),productId);}
 @DeleteMapping("/{productId}") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void remove(Principal user,@PathVariable Long productId){favorites.remove(user.getName(),productId);}
}
