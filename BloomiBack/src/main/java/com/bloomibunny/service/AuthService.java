package com.bloomibunny.service;
import com.bloomibunny.dto.AuthDtos.*;
public interface AuthService {AuthResponse login(LoginRequest request);AuthResponse register(RegisterRequest request);}
