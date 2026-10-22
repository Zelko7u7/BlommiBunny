package com.bloomibunny.dto;
import com.bloomibunny.entity.Role;
import jakarta.validation.constraints.*;
public final class AuthDtos {
 private AuthDtos(){}
 public record LoginRequest(@NotBlank String username,@NotBlank String password){}
 public record RegisterRequest(@NotBlank @Size(max=80) String username,@NotBlank @Size(min=8,max=100) String password,Role role){}
 public record SignupRequest(@NotBlank @Size(max=80) String username,@NotBlank @Size(min=8,max=100) String password){}
 public record AuthResponse(String token,String username,Role role){}
}
