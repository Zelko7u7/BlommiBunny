package com.bloomibunny.security;
import com.bloomibunny.controller.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.repository.AppUserRepository;
import com.bloomibunny.service.*;
import com.bloomibunny.dto.BannerDtos.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringJUnitConfig(BannerAccessTest.Config.class) @WebAppConfiguration
class BannerAccessTest {
 @Configuration @EnableWebMvc @EnableWebSecurity @Import({SecurityConfig.class,BannerController.class,BannerImageController.class,com.bloomibunny.exception.ApiExceptionHandler.class})
 static class Config {
  @Bean BannerService banners(){return mock(BannerService.class);}@Bean CloudinaryImageStorage cloud(){return mock(CloudinaryImageStorage.class);}@Bean AppUserRepository users(){return mock(AppUserRepository.class);}@Bean JwtService jwt(){return mock(JwtService.class);}@Bean JwtAuthFilter filter(JwtService j,AppUserRepository u){return new JwtAuthFilter(j,u);}
  @Bean static org.springframework.context.support.PropertySourcesPlaceholderConfigurer properties(){var c=new org.springframework.context.support.PropertySourcesPlaceholderConfigurer();var p=new Properties();p.setProperty("app.cors.allowed-origin","http://localhost:5173");p.setProperty("jwt.secret","banner-fixture-signing-key-never-used-remotely");p.setProperty("jwt.expiration-ms","86400000");c.setProperties(p);return c;}
 }
 @Autowired WebApplicationContext context;@Autowired BannerService banners;@Autowired CloudinaryImageStorage cloud;MockMvc mvc;
 @BeforeEach void setup(){reset(banners,cloud);mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
 @Test void publicCanReadOnlyPublishedList()throws Exception{when(banners.published()).thenReturn(List.of());mvc.perform(get("/api/banners")).andExpect(status().isOk()).andExpect(content().json("[]")).andExpect(header().string("Cache-Control","no-store"));verify(banners).published();verify(banners,never()).list();}
 void denied(int status)throws Exception{
  for(String path:List.of("/api/admin/banners","/api/admin/banners/targets"))mvc.perform(get(path)).andExpect(status().is(status));
  mvc.perform(post("/api/admin/banners").contentType("application/json").content("{}")).andExpect(status().is(status));mvc.perform(put("/api/admin/banners/1").contentType("application/json").content("{}")).andExpect(status().is(status));mvc.perform(delete("/api/admin/banners/1?expectedVersion=0")).andExpect(status().is(status));mvc.perform(patch("/api/admin/banners/1/status").contentType("application/json").content("{}")).andExpect(status().is(status));mvc.perform(patch("/api/admin/banners/reorder").contentType("application/json").content("{}")).andExpect(status().is(status));mvc.perform(multipart("/api/admin/banners/images").file(new MockMultipartFile("file","x.png","image/png",new byte[]{1}))).andExpect(status().is(status));verifyNoInteractions(banners,cloud);
 }
 @Test void guestCannotManageAnything()throws Exception{denied(401);}
 @Test @WithMockUser(roles="CUSTOMER") void customerCannotManageAnything()throws Exception{denied(403);}
 @Test @WithMockUser(roles="EMPLOYEE") void employeeCannotManageAnything()throws Exception{denied(403);}
 @Test @WithMockUser(roles="ADMIN") void adminCanReadManageAndInvalidBodiesNeverReachService()throws Exception{mvc.perform(get("/api/admin/banners")).andExpect(status().isOk());mvc.perform(post("/api/admin/banners").contentType("application/json").content("{}")).andExpect(status().isBadRequest());mvc.perform(delete("/api/admin/banners/1?expectedVersion=0")).andExpect(status().isNoContent());mvc.perform(patch("/api/admin/banners/1/status").contentType("application/json").content("{\"active\":false,\"expectedVersion\":0}")).andExpect(status().isOk());verify(banners).delete(1L,0L);verify(banners).status(eq(1L),any());verify(banners,never()).create(any());}
 @Test @WithMockUser(roles="ADMIN") void invalidImageCannotReachProvider()throws Exception{when(cloud.enabled()).thenReturn(true);mvc.perform(multipart("/api/admin/banners/images").file(new MockMultipartFile("file","fake.png","image/png","<script>".getBytes()))).andExpect(status().isBadRequest());verify(cloud,never()).storeBanner(any(),any(),any());}
}
