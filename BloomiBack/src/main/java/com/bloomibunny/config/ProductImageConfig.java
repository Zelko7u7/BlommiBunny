package com.bloomibunny.config;

import com.bloomibunny.service.ProductImageStorage;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.*;

@Configuration
@PropertySource(value="file:${BLOOMI_MEDIA_CONFIG:../.local/cloudinary.properties}",ignoreResourceNotFound=true)
public class ProductImageConfig {
 @Bean MultipartConfigElement productImageMultipartConfig(){
  // Allow multipart headers in addition to the 5 MB file, one file per request.
  return new MultipartConfigElement("",ProductImageStorage.MAX_BYTES,6L*1024*1024,0);
 }
}
