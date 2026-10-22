package com.bloomibunny.security;

import com.bloomibunny.controller.*;
import com.bloomibunny.dto.AuthDtos.*;
import com.bloomibunny.dto.ProductDtos.ProductResponse;
import com.bloomibunny.entity.AppUser;
import com.bloomibunny.entity.Role;
import com.bloomibunny.repository.AppUserRepository;
import com.bloomibunny.service.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(AccessControlTest.Config.class)
@WebAppConfiguration
class AccessControlTest {
 @Configuration @EnableWebMvc @EnableWebSecurity
 @Import({SecurityConfig.class,ProductController.class,StoreController.class,AuthController.class,FavoriteController.class,com.bloomibunny.exception.ApiExceptionHandler.class})
 static class Config {
  @Bean ProductService products(){return mock(ProductService.class);}
  @Bean AuthService auth(){return mock(AuthService.class);}
  @Bean FavoriteService favorites(){return mock(FavoriteService.class);}
  @Bean AppUserRepository users(){return mock(AppUserRepository.class);}
  @Bean JwtService jwt(){var jwt=new JwtService();ReflectionTestUtils.setField(jwt,"secret","test-bloomi-secret-long-enough-for-hmac-256-signing");ReflectionTestUtils.setField(jwt,"expiration",86400000L);return jwt;}
  @Bean JwtAuthFilter filter(JwtService jwt,AppUserRepository users){return new JwtAuthFilter(jwt,users);}
  @Bean static org.springframework.context.support.PropertySourcesPlaceholderConfigurer properties(){var config=new org.springframework.context.support.PropertySourcesPlaceholderConfigurer();var p=new Properties();p.setProperty("app.cors.allowed-origin","http://localhost:5173");p.setProperty("jwt.secret","test-bloomi-secret-long-enough-for-hmac-256-signing");p.setProperty("jwt.expiration-ms","86400000");config.setProperties(p);return config;}
 }

 @Autowired WebApplicationContext context;
 @Autowired ProductService products;
 @Autowired AuthService auth;
 @Autowired FavoriteService favorites;
 @Autowired AppUserRepository users;
 @Autowired JwtService jwt;
 MockMvc mvc;

 @BeforeEach void setup(){
  reset(products,auth,users,favorites);
  mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  var product=new ProductResponse(1L,"Gloss","GLOSS-1",1L,"Labiales",null,null,2L,"Proveedor privado","Descripción",null,null,new BigDecimal("5000"),new BigDecimal("12000"),new BigDecimal("7000"),3,1,null,true,"/products/gloss.png","OK",false,true,0L,null);
  when(products.list(anyString(),anyInt(),anyInt(),anyString())).thenReturn(new PageImpl<>(List.of(product),org.springframework.data.domain.PageRequest.of(0,20),1));
  when(products.get(1L)).thenReturn(product);
 }

 @Test void guestCanBrowseWithoutSeeingInternalData()throws Exception{
  mvc.perform(get("/api/store/products")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].salePrice").value(12000)).andExpect(jsonPath("$.content[0].purchasePrice").doesNotExist()).andExpect(jsonPath("$.content[0].profit").doesNotExist()).andExpect(jsonPath("$.content[0].supplier").doesNotExist());
  mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/products/summary")).andExpect(status().isUnauthorized());
 }

 @Test void guestsCannotReadOrChangeFavorites()throws Exception{
  mvc.perform(get("/api/favorites")).andExpect(status().isUnauthorized());
  mvc.perform(put("/api/favorites/1")).andExpect(status().isUnauthorized());
  mvc.perform(delete("/api/favorites/1")).andExpect(status().isUnauthorized());
  verifyNoInteractions(favorites);
 }

 @Test @WithMockUser(username="cliente",roles="CUSTOMER") void favoritesAlwaysUseAuthenticatedAccount()throws Exception{
  when(favorites.list("cliente")).thenReturn(List.of());
  mvc.perform(get("/api/favorites?username=otra-persona")).andExpect(status().isOk()).andExpect(content().json("[]"));
  mvc.perform(put("/api/favorites/1?username=otra-persona")).andExpect(status().isNoContent());
  mvc.perform(delete("/api/favorites/1?username=otra-persona")).andExpect(status().isNoContent());
  verify(favorites).list("cliente");verify(favorites).add("cliente",1L);verify(favorites).remove("cliente",1L);
  verifyNoMoreInteractions(favorites);
 }

 @Test void productDetailsArePublicAndContainOnlyCommercialData()throws Exception{
  mvc.perform(get("/api/store/products/1")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Gloss")).andExpect(jsonPath("$.salePrice").value(12000)).andExpect(jsonPath("$.description").value("Descripción")).andExpect(jsonPath("$.purchasePrice").doesNotExist()).andExpect(jsonPath("$.profit").doesNotExist()).andExpect(jsonPath("$.supplier").doesNotExist()).andExpect(jsonPath("$.costConfirmed").doesNotExist());
 }

 @Test void disabledAndDeletedProductLinksReturnNotFound()throws Exception{
  var disabled=new ProductResponse(2L,"Gloss oculto","HIDDEN",1L,"Labiales",null,null,null,null,null,null,null,new BigDecimal("5000"),new BigDecimal("12000"),new BigDecimal("7000"),3,1,null,false,null,"OK",false,true,0L,null);
  when(products.get(2L)).thenReturn(disabled);
  when(products.get(3L)).thenThrow(new com.bloomibunny.exception.ResourceNotFoundException("Producto no encontrado"));
  mvc.perform(get("/api/store/products/2")).andExpect(status().isNotFound());
  mvc.perform(get("/api/store/products/3")).andExpect(status().isNotFound());
 }

 ProductResponse variant(Long id,String shade){return new ProductResponse(id,"Labial Doble Vaquero Toy Story","TONE-"+id,1L,"Labios",null,null,2L,"Proveedor privado",null,shade,null,new BigDecimal("10000"),new BigDecimal("15000"),new BigDecimal("5000"),4,1,null,true,null,"OK",false,true,0L,"toy-story-labial");}

 @Test void variantDetailsExposeAvailableTonesWithoutPrivateInventoryData()throws Exception{
  var tone2=variant(11L,"Tono 2");var tone3=variant(12L,"Tono 3");
  when(products.get(11L)).thenReturn(tone2);when(products.variants("toy-story-labial")).thenReturn(List.of(tone2,tone3));
  mvc.perform(get("/api/store/products/11")).andExpect(status().isOk())
   .andExpect(jsonPath("$.name").value("Labial Doble Vaquero Toy Story"))
   .andExpect(jsonPath("$.variants.length()").value(2))
   .andExpect(jsonPath("$.variants[0].shade").value("Tono 2"))
   .andExpect(jsonPath("$.variants[1].id").value(12))
   .andExpect(jsonPath("$.variants[1].stock").value(4))
   .andExpect(jsonPath("$.variants[0].purchasePrice").doesNotExist())
   .andExpect(jsonPath("$.variants[0].supplier").doesNotExist())
   .andExpect(jsonPath("$.variants[0].profit").doesNotExist());
 }

 @Test @WithMockUser(roles="CUSTOMER") void customerCannotReadOrWriteAdministration()throws Exception{
  for(String path:List.of("/api/products","/api/products/summary","/api/dashboard","/api/categories","/api/brands","/api/suppliers","/api/customers","/api/sales","/api/purchases","/api/expenses"))mvc.perform(get(path)).andExpect(status().isForbidden());
  mvc.perform(post("/api/products").contentType("application/json").content("{}")).andExpect(status().isForbidden());
  mvc.perform(put("/api/products/1").contentType("application/json").content("{}")).andExpect(status().isForbidden());
  mvc.perform(patch("/api/products/1/stock").contentType("application/json").content("{\"quantity\":100,\"reason\":\"test\"}")).andExpect(status().isForbidden());
  mvc.perform(patch("/api/products/1/status").contentType("application/json").content("{\"active\":true}")).andExpect(status().isForbidden());
  mvc.perform(delete("/api/products/1")).andExpect(status().isForbidden());
  mvc.perform(post("/api/auth/register").contentType("application/json").content("{}")).andExpect(status().isForbidden());
  verifyNoInteractions(products,auth);
 }

 @Test @WithMockUser(roles="EMPLOYEE") void legacyEmployeeCannotEnterAdministration()throws Exception{
  mvc.perform(get("/api/products")).andExpect(status().isForbidden());
  mvc.perform(get("/api/dashboard")).andExpect(status().isForbidden());
 }

 @Test @WithMockUser(roles="ADMIN") void adminCanReadPrivateProductData()throws Exception{
  mvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].purchasePrice").value(5000));
 }

 @Test void publicSignupAlwaysCreatesCustomer()throws Exception{
  when(auth.register(any())).thenReturn(new AuthResponse("token","cliente",Role.CUSTOMER));
  mvc.perform(post("/api/auth/signup").contentType("application/json").content("{\"username\":\"cliente\",\"password\":\"password123\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("CUSTOMER"));
  verify(auth).register(new RegisterRequest("cliente","password123",Role.CUSTOMER));
 }

 @Test void tokenDoesNotKeepOldAdminPrivileges()throws Exception{
  var user=new AppUser();user.setUsername("oldadmin");user.setRole(Role.ADMIN);
  String token=jwt.generate(user);
  user.setRole(Role.CUSTOMER);
  when(users.findByUsername("oldadmin")).thenReturn(Optional.of(user));
  mvc.perform(get("/api/products").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
  user.setActive(false);
  mvc.perform(get("/api/products").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
 }
}
