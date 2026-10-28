package com.bloomibunny.service;

import com.bloomibunny.exception.BusinessException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Ordered secondary photos. A missing field preserves photos from older clients. */
public final class ProductMedia {
 private ProductMedia() {}
 public static List<String> normalize(List<String> images,String cover) {
  if(images==null)return null;
  if(images.size()>20)throw new BusinessException("La galería admite hasta 20 fotografías secundarias.");
  var unique=new LinkedHashSet<String>();
  for(var value:images){
   if(value==null||value.isBlank()||value.length()>500)throw new BusinessException("Revisa las rutas de las fotografías de la galería.");
   var url=value.trim();
   try{
    var uri=URI.create(url);
    boolean local=url.startsWith("/products/")&&!url.contains("\\")&&!uri.normalize().getPath().contains("..")&&uri.normalize().getPath().startsWith("/products/");
    boolean remote=("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))&&uri.getHost()!=null&&uri.getUserInfo()==null;
    if(!local&&!remote)throw new IllegalArgumentException();
   }catch(IllegalArgumentException e){throw new BusinessException("Usa rutas /products/ o enlaces HTTP/HTTPS válidos para las fotografías.");}
   if(!url.equals(cover))unique.add(url);
  }
  return new ArrayList<>(unique);
 }
}
