package com.bloomibunny.service;

import com.bloomibunny.exception.ProductImageException;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

/** Signed server-side uploads; credentials and signatures never reach the browser. */
@Service
public class CloudinaryImageStorage {
 private final boolean enabled;
 private final String cloud,apiKey,secret;
 private final HttpClient client;
 private final ObjectMapper mapper;
 private final Clock clock;
 @Autowired
 public CloudinaryImageStorage(@Value("${CLOUDINARY_ENABLED:${cloudinary.enabled:false}}") boolean enabled,
    @Value("${CLOUDINARY_CLOUD_NAME:${cloudinary.cloud-name:}}") String cloud,
    @Value("${CLOUDINARY_API_KEY:${cloudinary.api-key:}}") String apiKey,
    @Value("${CLOUDINARY_API_SECRET:${cloudinary.api-secret:}}") String secret,ObjectMapper mapper){
  this(enabled,cloud,apiKey,secret,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build(),mapper,Clock.systemUTC());
 }
 CloudinaryImageStorage(boolean enabled,String cloud,String apiKey,String secret,HttpClient client,ObjectMapper mapper,Clock clock){
  this.enabled=enabled;this.cloud=cloud.trim();this.apiKey=apiKey.trim();this.secret=secret.trim();this.client=client;this.mapper=mapper;this.clock=clock;
 }
 public boolean enabled(){return enabled;}
 public ProductImageStorage.StoredImage store(byte[] data,String format,String contentType){
  var image=upload(data,format,contentType,"bloomibunny/products",false);
  return new ProductImageStorage.StoredImage(null,image.url(),contentType,data.length);
 }
 public record CloudImage(String url,String publicId,int width,int height){}
 public CloudImage storeBanner(byte[] data,String format,String contentType){return upload(data,format,contentType,"bloomi-bunny/banners",true);}
 public boolean validBannerImage(String url,String publicId){
  if(url==null||publicId==null||!publicId.matches("bloomi-bunny/banners/[a-zA-Z0-9_-]+"))return false;
  try{var uri=URI.create(url);return url.length()<=500&&"https".equals(uri.getScheme())&&"res.cloudinary.com".equals(uri.getHost())&&uri.getUserInfo()==null&&uri.getPort()==-1&&uri.getQuery()==null&&uri.getFragment()==null&&uri.getPath().matches("/"+java.util.regex.Pattern.quote(cloud)+"/image/upload/v[0-9]+/"+java.util.regex.Pattern.quote(publicId)+"\\.(jpg|jpeg|png|webp)");}catch(RuntimeException e){return false;}
 }
 private CloudImage upload(byte[] data,String format,String contentType,String folder,boolean banner){
  if(!enabled||!cloud.matches("[a-zA-Z0-9_-]+")||apiKey.isBlank()||secret.isBlank())throw new ProductImageException(HttpStatus.SERVICE_UNAVAILABLE,"Completa la configuración privada de Cloudinary para poder subir fotografías.");
  String publicId=folder+"/"+UUID.randomUUID();
  var fields=new TreeMap<String,String>();
  if(banner)fields.put("asset_folder",folder);
  fields.put("overwrite","false");fields.put("public_id",publicId);fields.put("timestamp",Long.toString(clock.instant().getEpochSecond()));
  String boundary="bloomi-"+UUID.randomUUID();
  try{
   String signed=String.join("&",fields.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).toList());
   String signature=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((signed+secret).getBytes(StandardCharsets.UTF_8)));
   fields.put("api_key",apiKey);fields.put("signature",signature);
   var body=new ByteArrayOutputStream(data.length+2048);
   for(var field:fields.entrySet()){
    body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+field.getKey()+"\"\r\n\r\n"+field.getValue()+"\r\n").getBytes(StandardCharsets.UTF_8));
   }
   body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"photo."+format+"\"\r\nContent-Type: "+contentType+"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
   body.write(data);body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
   var request=HttpRequest.newBuilder(URI.create("https://api.cloudinary.com/v1_1/"+cloud+"/image/upload"))
    .timeout(Duration.ofSeconds(22)).header("Content-Type","multipart/form-data; boundary="+boundary)
    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
   var response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());
   try(var stream=response.body()){
    byte[] reply=stream.readNBytes(65537);
    if(response.statusCode()==401||response.statusCode()==403)throw new ProductImageException(HttpStatus.SERVICE_UNAVAILABLE,"No pudimos conectar con el servicio de imágenes. Revisa la configuración privada de Cloudinary.");
    if(response.statusCode()<200||response.statusCode()>=300||reply.length>65536)throw unavailable();
    var result=mapper.readTree(reply);
    String url=result.path("secure_url").asString("");
    URI uri=URI.create(url);
    if(!publicId.equals(result.path("public_id").asString(""))||!Set.of("jpg","jpeg","png","webp").contains(result.path("format").asString(""))
       ||!"https".equals(uri.getScheme())||!"res.cloudinary.com".equals(uri.getHost())||uri.getUserInfo()!=null||uri.getPort()!=-1
       ||!uri.getPath().startsWith("/"+cloud+"/image/upload/")||!uri.getPath().contains("/"+publicId+".")||url.length()>500)throw unavailable();
    return new CloudImage(url,publicId,result.path("width").asInt(0),result.path("height").asInt(0));
   }
  }catch(ProductImageException error){throw error;}
  catch(InterruptedException error){Thread.currentThread().interrupt();throw unavailable();}
  catch(IOException|NoSuchAlgorithmException|RuntimeException error){throw unavailable();}
 }
 private static ProductImageException unavailable(){return new ProductImageException(HttpStatus.BAD_GATEWAY,"No pudimos subir la fotografía al servicio de imágenes. Inténtalo de nuevo.");}
}
