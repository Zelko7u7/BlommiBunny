package com.bloomibunny.service;

import com.bloomibunny.exception.ProductImageException;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;
import java.io.*;
import java.net.http.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import java.util.regex.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CloudinaryImageStorageTest {
 HttpClient client=mock(HttpClient.class);
 Clock clock=Clock.fixed(Instant.ofEpochSecond(1700000000),ZoneOffset.UTC);
 CloudinaryImageStorage storage(){return new CloudinaryImageStorage(true,"test-cloud","fixture-key","fixture-secret",client,new JsonMapper(),clock);}
 String body(HttpRequest request){
  var output=new ByteArrayOutputStream();var complete=new CompletableFuture<String>();
  request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>(){
   public void onSubscribe(Flow.Subscription subscription){subscription.request(Long.MAX_VALUE);}
   public void onNext(ByteBuffer bytes){byte[] chunk=new byte[bytes.remaining()];bytes.get(chunk);output.writeBytes(chunk);}
   public void onError(Throwable error){complete.completeExceptionally(error);}
   public void onComplete(){complete.complete(output.toString(StandardCharsets.UTF_8));}
  });return complete.join();
 }
 String field(String body,String name){var matcher=Pattern.compile("name=\""+name+"\"\\r\\n\\r\\n([^\\r]+)").matcher(body);assertTrue(matcher.find(),name);return matcher.group(1);}
 @SuppressWarnings("unchecked") HttpResponse<InputStream> reply(int status,String json){
  var response=(HttpResponse<InputStream>)mock(HttpResponse.class);when(response.statusCode()).thenReturn(status);when(response.body()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));return response;
 }
 @Test void signsUploadOnServerAndReturnsOnlyVerifiedHttpsUrl()throws Exception{
  when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(invocation->{
   HttpRequest request=invocation.getArgument(0);String content=body(request),publicId=field(content,"public_id");
   assertEquals("https://api.cloudinary.com/v1_1/test-cloud/image/upload",request.uri().toString());
   assertEquals("false",field(content,"overwrite"));assertEquals("fixture-key",field(content,"api_key"));assertEquals("1700000000",field(content,"timestamp"));
   String signed="overwrite=false&public_id="+publicId+"&timestamp=1700000000fixture-secret";
   assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(signed.getBytes(StandardCharsets.UTF_8))),field(content,"signature"));
   assertFalse(content.contains("fixture-secret"));assertTrue(publicId.startsWith("bloomibunny/products/"));
   return reply(200,"{\"public_id\":\""+publicId+"\",\"format\":\"png\",\"secure_url\":\"https://res.cloudinary.com/test-cloud/image/upload/v1/"+publicId+".png\"}");
  });
  var result=storage().store(new byte[]{1,2,3},"png","image/png");assertNull(result.fileName());assertTrue(result.imageUrl().startsWith("https://res.cloudinary.com/"));
 }
 @Test void missingOrInvalidCredentialsNeverSendARequest(){
  var storage=new CloudinaryImageStorage(true,"bad/cloud","","",client,new JsonMapper(),clock);
  assertEquals(503,assertThrows(ProductImageException.class,()->storage.store(new byte[1],"png","image/png")).status().value());verifyNoInteractions(client);
 }
 @Test void providerErrorsDoNotExposeCredentialsOrRawResponses()throws Exception{
  var response=reply(401,"fixture-secret");
  when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
  var error=assertThrows(ProductImageException.class,()->storage().store(new byte[1],"png","image/png"));assertEquals(503,error.status().value());assertFalse(error.getMessage().contains("fixture-secret"));
 }
 @Test void invalidExternalResponseIsRejected()throws Exception{
  var response=reply(200,"{\"format\":\"png\",\"secure_url\":\"https://evil.invalid/photo.png\"}");
  when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
  assertEquals(502,assertThrows(ProductImageException.class,()->storage().store(new byte[1],"png","image/png")).status().value());
 }
 @Test void networkErrorsAreRecoverableAndSanitized()throws Exception{
  when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("fixture-secret"));
  var error=assertThrows(ProductImageException.class,()->storage().store(new byte[1],"png","image/png"));assertEquals(502,error.status().value());assertFalse(error.getMessage().contains("fixture-secret"));
 }
}
