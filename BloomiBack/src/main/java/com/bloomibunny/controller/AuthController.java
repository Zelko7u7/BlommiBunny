package com.bloomibunny.controller;
import com.bloomibunny.dto.AuthDtos.*;
import com.bloomibunny.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final AuthService service;
 public AuthController(AuthService s){service=s;}
 @PostMapping("/login") AuthResponse login(@Valid @RequestBody LoginRequest r){return service.login(r);}
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) AuthResponse register(@Valid @RequestBody RegisterRequest r){return service.register(r);}
 @PostMapping("/signup") @ResponseStatus(HttpStatus.CREATED) AuthResponse signup(@Valid @RequestBody SignupRequest r){return service.register(new RegisterRequest(r.username(),r.password(),com.bloomibunny.entity.Role.CUSTOMER));}
}
