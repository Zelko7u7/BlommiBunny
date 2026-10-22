package com.bloomibunny.service.impl;
import com.bloomibunny.dto.AuthDtos.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.repository.AppUserRepository;
import com.bloomibunny.security.JwtService;
import com.bloomibunny.service.AuthService;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional
public class AuthServiceImpl implements AuthService {
 private final AppUserRepository repo;private final PasswordEncoder encoder;private final AuthenticationManager auth;private final JwtService jwt;
 public AuthServiceImpl(AppUserRepository r,PasswordEncoder e,AuthenticationManager a,JwtService j){repo=r;encoder=e;auth=a;jwt=j;}
 public AuthResponse login(LoginRequest r){auth.authenticate(new UsernamePasswordAuthenticationToken(r.username(),r.password()));var u=repo.findByUsername(r.username()).orElseThrow();return new AuthResponse(jwt.generate(u),u.getUsername(),u.getRole());}
 public AuthResponse register(RegisterRequest r){String username=r.username().trim();if(username.isBlank())throw new IllegalArgumentException("El usuario no puede estar vacío");if(repo.findByUsername(username).isPresent())throw new IllegalArgumentException("El usuario ya existe");var u=new AppUser();u.setUsername(username);u.setPassword(encoder.encode(r.password()));u.setRole(r.role()==null?Role.CUSTOMER:r.role());repo.save(u);return new AuthResponse(jwt.generate(u),u.getUsername(),u.getRole());}
}
