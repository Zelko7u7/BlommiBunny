package com.bloomibunny.controller;
import com.bloomibunny.dto.BannerDtos.*;
import com.bloomibunny.service.BannerService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
public class BannerController {
 private final BannerService service;
 public BannerController(BannerService s){service=s;}
 @GetMapping("/api/banners") public ResponseEntity<List<PublicBanner>> published(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.published());}
 @GetMapping("/api/admin/banners") public List<Response> list(){return service.list();}
 @GetMapping("/api/admin/banners/targets") public Targets targets(){return service.targets();}
 @PostMapping("/api/admin/banners") @ResponseStatus(HttpStatus.CREATED) public Response create(@Valid @RequestBody Request r){return service.create(r);}
 @PutMapping("/api/admin/banners/{id}") public Response update(@PathVariable Long id,@Valid @RequestBody Request r){return service.update(id,r);}
 @PatchMapping("/api/admin/banners/{id}/status") public Response status(@PathVariable Long id,@Valid @RequestBody Status r){return service.status(id,r);}
 @PatchMapping("/api/admin/banners/reorder") public List<Response> reorder(@Valid @RequestBody Reorder r){return service.reorder(r);}
 @DeleteMapping("/api/admin/banners/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id,@RequestParam long expectedVersion){service.delete(id,expectedVersion);}
}
