package com.bloomibunny.controller;

import com.bloomibunny.service.ProductImageStorage;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class ProductImageController {
 private final ProductImageStorage storage;
 public ProductImageController(ProductImageStorage storage){this.storage=storage;}
 public record ImageResponse(String imageUrl,String contentType,long size){}
 @PostMapping(value="/api/media/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 @ResponseStatus(HttpStatus.CREATED)
 public ImageResponse upload(@RequestPart("file") MultipartFile file){
  var image=storage.store(file);
  var url=image.imageUrl()!=null?image.imageUrl():ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/store/media/").path(image.fileName()).build().toUriString();
  return new ImageResponse(url,image.contentType(),image.size());
 }
 @GetMapping("/api/store/media/{name}")
 public ResponseEntity<Resource> get(@PathVariable String name){
  var file = storage.file(name);
  return ResponseEntity.ok().contentType(MediaType.parseMediaType(storage.contentType(name)))
   .header(HttpHeaders.CACHE_CONTROL,"public, max-age=31536000, immutable")
   .header("X-Content-Type-Options","nosniff").body(new FileSystemResource(file));
 }
}
