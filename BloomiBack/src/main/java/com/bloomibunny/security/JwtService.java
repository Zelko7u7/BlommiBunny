package com.bloomibunny.security;
import com.bloomibunny.entity.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
@Service
public class JwtService {
 @Value("${jwt.secret}") private String secret; @Value("${jwt.expiration-ms}") private long expiration;
 private SecretKey key(){return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));}
 public String generate(AppUser u){return Jwts.builder().subject(u.getUsername()).claim("role",u.getRole().name()).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+expiration)).signWith(key()).compact();}
 public Claims claims(String token){return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();}
 public boolean valid(String token){try{claims(token);return true;}catch(JwtException|IllegalArgumentException e){return false;}}
}
