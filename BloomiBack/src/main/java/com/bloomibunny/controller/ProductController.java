package com.bloomibunny.controller;
import com.bloomibunny.dto.ProductDtos.*;
import com.bloomibunny.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/products")
public class ProductController {
 private final ProductService service;public ProductController(ProductService s){service=s;}
 @GetMapping Page<ProductResponse> list(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(defaultValue="name")String sort){return service.list(q,page,size,sort);}
 @GetMapping("/summary") com.bloomibunny.dto.InventorySummary summary(@RequestParam(defaultValue="")String q){return service.summary(q);}
 @GetMapping("/{id}") ProductResponse get(@PathVariable Long id){return service.get(id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) ProductResponse create(@Valid @RequestBody ProductRequest r){return service.create(r);}
 @PutMapping("/{id}") ProductResponse update(@PathVariable Long id,@Valid @RequestBody ProductRequest r){return service.update(id,r);}
 @PatchMapping("/{id}/stock") ProductResponse stock(@PathVariable Long id,@Valid @RequestBody StockAdjustment r){return service.adjust(id,r);}
 @PatchMapping("/{id}/status") ProductResponse status(@PathVariable Long id,@Valid @RequestBody ProductStatus r){return service.setActive(id,r.active());}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable Long id){service.delete(id);}
}
