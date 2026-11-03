package com.bloomibunny.controller;

import com.bloomibunny.service.InventoryLotService;
import com.bloomibunny.repository.ProductRepository;
import com.bloomibunny.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/products")
public class InventoryLotController {
 private final InventoryLotService lots;private final ProductRepository products;
 public InventoryLotController(InventoryLotService lots,ProductRepository products){this.lots=lots;this.products=products;}
 @GetMapping("/{id}/lots") @Transactional(readOnly=true)
 public List<InventoryLotService.LotResponse> list(@PathVariable Long id){return lots.list(products.findById(id).filter(p->!p.isDeleted()).orElseThrow(()->new ResourceNotFoundException("Producto no encontrado")));}
}
