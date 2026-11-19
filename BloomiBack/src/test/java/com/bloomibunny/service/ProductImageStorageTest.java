package com.bloomibunny.service;

import com.bloomibunny.exception.ProductImageException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductImageStorageTest {
 @TempDir Path directory;
 CloudinaryImageStorage cloud=mock(CloudinaryImageStorage.class);
 ProductImageStorage storage(){return new ProductImageStorage(directory.toString(),true,"",cloud);}
 byte[] raster(String format)throws Exception{
  var data=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,3,BufferedImage.TYPE_INT_RGB),format,data);return data.toByteArray();
 }
 @Test void storesOriginalPngWithGeneratedNameAndNoDatabase()throws Exception{
  var data=raster("png");var storage=storage();var result=storage.store(new MockMultipartFile("file","../../private.svg","image/png",data));
  assertTrue(result.fileName().matches("[a-f0-9-]{36}\\.png"));assertNull(result.imageUrl());assertEquals(data.length,result.size());
  assertArrayEquals(data,Files.readAllBytes(storage.file(result.fileName())));assertEquals("image/png",storage.contentType(result.fileName()));
  verify(cloud,never()).store(any(),any(),any());
 }
 @Test void separateUploadsNeverOverwriteEachOther()throws Exception{
  var file=new MockMultipartFile("file","same.jpg","image/jpeg",raster("jpg"));var storage=storage();
  var first=storage.store(file);var second=storage.store(file);assertNotEquals(first.fileName(),second.fileName());assertEquals(2,Files.list(directory).count());
 }
 @Test void rejectsEmptyOversizedAndSpoofedFiles()throws Exception{
  var storage=storage();
  for(var file:List.of(new MockMultipartFile("file","empty.png","image/png",new byte[0]),new MockMultipartFile("file","fake.jpg","image/jpeg","<svg/>".getBytes()),new MockMultipartFile("file","fake.jpg","image/jpeg",raster("png"))))assertThrows(ProductImageException.class,()->storage.store(file));
  var error=assertThrows(ProductImageException.class,()->storage.store(new MockMultipartFile("file","large.png","image/png",new byte[ProductImageStorage.MAX_BYTES+1])));
  assertEquals(413,error.status().value());assertEquals(0,Files.list(directory).count());
 }
 @Test void rejectsDamagedRasterBeforeWriting(){
  var file=new MockMultipartFile("file","broken.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10,0});
  assertThrows(ProductImageException.class,()->storage().store(file));
 }
 @Test void rejectsUnsupportedSvgGifAndUnknownTypes(){
  for(var mime:List.of("image/svg+xml","image/gif","text/html"))assertThrows(ProductImageException.class,()->storage().store(new MockMultipartFile("file","photo",mime,"<svg/>".getBytes())));
 }
 @Test void validatesWebpContainerAndDimensions(){
  byte[] data=Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");
  var file=new MockMultipartFile("file","photo.webp","image/webp",data);
  var result=storage().store(file);assertEquals("image/webp",result.contentType());
  assertThrows(ProductImageException.class,()->storage().store(new MockMultipartFile("file","broken.webp","image/webp",Arrays.copyOf(data,data.length-1))));
 }
 @Test void rejectsHugeDimensionsBeforeAllocatingPixels()throws Exception{
  byte[] data=raster("png");data[16]=0x7f;data[17]=(byte)255;data[18]=(byte)255;data[19]=(byte)255;
  assertThrows(ProductImageException.class,()->storage().store(new MockMultipartFile("file","huge.png","image/png",data)));
 }
 @Test void doesNotExposeFilesOutsideImageDirectory()throws Exception{
  var storage=storage();Files.writeString(directory.resolve("secret.properties"),"private");
  for(var name:List.of("../secret.properties","secret.properties","invalid.png",UUID.randomUUID()+".png"))assertThrows(ProductImageException.class,()->storage.file(name));
 }
 @Test void refusesEphemeralHerokuStorage()throws Exception{
  var storage=new ProductImageStorage(directory.toString(),true,"web.1",cloud);
  assertEquals(HttpStatus.SERVICE_UNAVAILABLE,assertThrows(ProductImageException.class,()->storage.store(new MockMultipartFile("file","photo.png","image/png",raster("png")))).status());
 }
 @Test void configuredCloudUploadsNeverWriteLocalFiles()throws Exception{
  when(cloud.enabled()).thenReturn(true);var expected=new ProductImageStorage.StoredImage(null,"https://res.cloudinary.com/test/image/upload/v1/photo.png","image/png",70);
  when(cloud.store(any(),eq("png"),eq("image/png"))).thenReturn(expected);
  var storage=new ProductImageStorage(directory.toString(),false,"web.1",cloud);
  assertSame(expected,storage.store(new MockMultipartFile("file","photo.png","image/png",raster("png"))));assertEquals(0,Files.list(directory).count());
 }
}
