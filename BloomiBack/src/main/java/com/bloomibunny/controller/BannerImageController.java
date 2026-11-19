package com.bloomibunny.controller;
import com.bloomibunny.dto.BannerDtos.Image;
import com.bloomibunny.exception.ProductImageException;
import com.bloomibunny.service.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.concurrent.Semaphore;
@RestController
public class BannerImageController {
 private final CloudinaryImageStorage cloud;private final Semaphore slots=new Semaphore(2);
 public BannerImageController(CloudinaryImageStorage c){cloud=c;}
 @PostMapping(value="/api/admin/banners/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
 public Image upload(@RequestPart("file") MultipartFile file){
  if(!cloud.enabled())throw new ProductImageException(HttpStatus.SERVICE_UNAVAILABLE,"Configura Cloudinary para subir imágenes de banners.");
  if(file.isEmpty())throw new ProductImageException(HttpStatus.BAD_REQUEST,"Selecciona una imagen.");
  if(file.getSize()>ProductImageStorage.MAX_BYTES)throw new ProductImageException(HttpStatus.PAYLOAD_TOO_LARGE,"Cada imagen admite hasta 5 MB.");
  if(!slots.tryAcquire())throw new ProductImageException(HttpStatus.TOO_MANY_REQUESTS,"Hay otras imágenes subiendo. Inténtalo de nuevo.");
  try(var input=file.getInputStream()){
   var data=input.readNBytes(ProductImageStorage.MAX_BYTES+1);if(data.length>ProductImageStorage.MAX_BYTES)throw new ProductImageException(HttpStatus.PAYLOAD_TOO_LARGE,"Cada imagen admite hasta 5 MB.");
   var format=ProductImageStorage.validateImage(data,file.getContentType());var image=cloud.storeBanner(data,format,"image/"+(format.equals("jpg")?"jpeg":format));
   if(image.width()<1||image.height()<1)throw new ProductImageException(HttpStatus.BAD_GATEWAY,"No pudimos verificar las dimensiones de la imagen.");
   return new Image(image.url(),image.publicId(),image.width(),image.height());
  }catch(IOException e){throw new ProductImageException(HttpStatus.SERVICE_UNAVAILABLE,"No pudimos leer la imagen. Inténtalo de nuevo.");}finally{slots.release();}
 }
}
