package com.bloomibunny.security;

import com.bloomibunny.dto.AuthDtos.RegisterRequest;
import com.bloomibunny.entity.*;
import com.bloomibunny.repository.AppUserRepository;
import com.bloomibunny.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationPolicyTest {
 @Test void serviceRejectsAllPrivilegedRolesBeforePersistence(){
  var users=mock(AppUserRepository.class);var encoder=mock(PasswordEncoder.class);var jwt=mock(JwtService.class);
  var service=new AuthServiceImpl(users,encoder,mock(AuthenticationManager.class),jwt);
  for(var role:new Role[]{Role.ADMIN,Role.EMPLOYEE})assertThrows(IllegalArgumentException.class,()->service.register(new RegisterRequest("intruso","fixture-password",role)));
  verifyNoInteractions(users,encoder,jwt);
 }
 @Test void customerRegistrationKeepsPasswordHashingAndReturnsCustomer(){
  var users=mock(AppUserRepository.class);var encoder=mock(PasswordEncoder.class);var jwt=mock(JwtService.class);
  when(users.findByUsername("cliente")).thenReturn(Optional.empty());when(encoder.encode("fixture-password")).thenReturn("hashed-password");when(jwt.generate(any())).thenReturn("fixture-token");
  var service=new AuthServiceImpl(users,encoder,mock(AuthenticationManager.class),jwt);
  for(var role:new Role[]{null,Role.CUSTOMER}){
   var response=service.register(new RegisterRequest(" cliente ","fixture-password",role));assertEquals(Role.CUSTOMER,response.role());
  }
  verify(users,times(2)).save(argThat(u->u.getRole()==Role.CUSTOMER&&u.getUsername().equals("cliente")&&u.getPassword().equals("hashed-password")));
 }
}
