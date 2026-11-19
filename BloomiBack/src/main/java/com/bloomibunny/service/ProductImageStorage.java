package com.bloomibunny.service;

import com.bloomibunny.exception.ProductImageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Semaphore;

/** Development storage, outside compiled/static assets. Cloud storage is required on Heroku. */
@Service
public class ProductImageStorage {
 public static final int MAX_BYTES=5*1024*1024;
 private static final long MAX_PIXELS=20_000_000;
 private static final String FILE_NAME="[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)";
 private final Path directory;
 private final boolean enabled;
 private final String dyno;
 private final CloudinaryImageStorage cloudinary;
 private final Semaphore slots=new Semaphore(2);
 public record StoredImage(String fileName,String imageUrl,String contentType,long size){}
 public ProductImageStorage(@Value("${app.media.local-directory:../.local/product-images}") String directory,
                            @Value("${app.media.local-enabled:true}") boolean enabled,
                            @Value("${DYNO:}") String dyno,CloudinaryImageStorage cloudinary){
  this.directory=Path.of(directory).toAbsolutePath().normalize();this.enabled=enabled;this.dyno=dyno;this.cloudinary=cloudinary;
 }
 public StoredImage store(MultipartFile file){
  if(!cloudinary.enabled()&&(!enabled||!dyno.isBlank()))throw failure(HttpStatus.SERVICE_UNAVAILABLE,"La subida de fotos necesita un almacenamiento persistente configurado para este entorno.");
  if(file==null||file.isEmpty())throw bad("Selecciona una fotografía para subir.");
  if(file.getSize()>MAX_BYTES)throw failure(HttpStatus.PAYLOAD_TOO_LARGE,"Cada fotografía debe pesar como máximo 5 MB.");
  if(!slots.tryAcquire())throw failure(HttpStatus.TOO_MANY_REQUESTS,"Hay otras fotografías subiendo. Inténtalo de nuevo en unos segundos.");
  try(var input=file.getInputStream()){
   byte[] data=input.readNBytes(MAX_BYTES+1);
   if(data.length>MAX_BYTES)throw failure(HttpStatus.PAYLOAD_TOO_LARGE,"Cada fotografía debe pesar como máximo 5 MB.");
   String format=validateImage(data,file.getContentType());
   String mime="image/"+(format.equals("jpg")?"jpeg":format);
   if(cloudinary.enabled())return cloudinary.store(data,format,mime);
   String name=UUID.randomUUID()+"."+format;
   Files.createDirectories(directory);
   Path target=directory.resolve(name);
   try{Files.write(target,data,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);}
   catch(FileAlreadyExistsException error){throw error;}
   catch(IOException error){Files.deleteIfExists(target);throw error;}
   return new StoredImage(name,null,mime,data.length);
  }catch(IOException error){throw failure(HttpStatus.SERVICE_UNAVAILABLE,"No pudimos guardar la fotografía. Inténtalo de nuevo.");}
  finally{slots.release();}
 }
 public Path file(String name){
  if(!name.matches(FILE_NAME))throw failure(HttpStatus.NOT_FOUND,"Fotografía no encontrada.");
  Path path=directory.resolve(name).normalize();
  if(!path.startsWith(directory)||!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))throw failure(HttpStatus.NOT_FOUND,"Fotografía no encontrada.");
  return path;
 }
 public String contentType(String name){return name.endsWith(".png")?"image/png":name.endsWith(".webp")?"image/webp":"image/jpeg";}
 public static String validateImage(byte[] data,String contentType){
  String format=format(data);String mime="image/"+(format.equals("jpg")?"jpeg":format);
  if(!mime.equalsIgnoreCase(Objects.toString(contentType,"")))throw bad("El contenido de la foto no coincide con su formato. Usa JPG, PNG o WebP.");
  if(format.equals("webp"))validateWebp(data);else validateRaster(data);
  return format;
 }
 private static String format(byte[] data){
  if(data.length>=8&&Arrays.equals(Arrays.copyOf(data,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))return "png";
  if(data.length>=3&&(data[0]&255)==255&&(data[1]&255)==216&&(data[2]&255)==255)return "jpg";
  if(data.length>=20&&ascii(data,0,"RIFF")&&ascii(data,8,"WEBP"))return "webp";
  throw bad("La fotografía no es válida. Usa un archivo JPG, PNG o WebP.");
 }
 private static void validateRaster(byte[] data){
  try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(data))){
   var readers=ImageIO.getImageReaders(stream);
   if(!readers.hasNext())throw bad("No pudimos leer la fotografía. Selecciona otra imagen.");
   var reader=readers.next();
   try{
    reader.setInput(stream,true,true);
    dimensions(reader.getWidth(0),reader.getHeight(0));
    var image=reader.read(0);
    if(image==null)throw bad("No pudimos leer la fotografía. Selecciona otra imagen.");
    image.flush();
   }finally{reader.dispose();}
  }catch(IOException|IllegalArgumentException error){throw bad("La fotografía está dañada o incompleta. Selecciona otra imagen.");}
 }
 private static void validateWebp(byte[] data){
  if(little(data,4,4)+8!=data.length)throw bad("La fotografía WebP está incompleta.");
  int offset=12;boolean pixels=false;
  while(offset+8<=data.length){
   long size=little(data,offset+4,4);int start=offset+8;
   if(size>data.length-start)throw bad("La fotografía WebP está incompleta.");
   if(ascii(data,offset,"VP8 ")){
    if(size<10||(data[start+3]&255)!=0x9d||data[start+4]!=1||data[start+5]!=42)throw bad("La fotografía WebP no es válida.");
    dimensions((int)little(data,start+6,2)&0x3fff,(int)little(data,start+8,2)&0x3fff);pixels=true;
   }else if(ascii(data,offset,"VP8L")){
    if(size<5||(data[start]&255)!=0x2f)throw bad("La fotografía WebP no es válida.");
    long bits=little(data,start+1,4);dimensions((int)(bits&0x3fff)+1,(int)((bits>>14)&0x3fff)+1);pixels=true;
   }else if(ascii(data,offset,"VP8X")){
    if(size!=10||(data[start]&2)!=0)throw bad("Usa una fotografía WebP sin animación.");
    dimensions((int)little(data,start+4,3)+1,(int)little(data,start+7,3)+1);
   }
   offset=(int)(start+size+(size&1));
  }
  if(!pixels||offset!=data.length)throw bad("La fotografía WebP no es válida o está animada.");
 }
 private static boolean ascii(byte[] data,int start,String value){
  if(start+value.length()>data.length)return false;
  for(int i=0;i<value.length();i++)if((data[start+i]&255)!=value.charAt(i))return false;
  return true;
 }
 private static long little(byte[] data,int start,int count){long value=0;for(int i=0;i<count;i++)value|=(long)(data[start+i]&255)<<(8*i);return value;}
 private static void dimensions(int width,int height){if(width<1||height<1||width>10000||height>10000||(long)width*height>MAX_PIXELS)throw bad("La fotografía es demasiado grande. Usa una imagen de hasta 20 megapíxeles y 10000 píxeles por lado.");}
 private static ProductImageException bad(String message){return failure(HttpStatus.BAD_REQUEST,message);}
 private static ProductImageException failure(HttpStatus status,String message){return new ProductImageException(status,message);}
}
