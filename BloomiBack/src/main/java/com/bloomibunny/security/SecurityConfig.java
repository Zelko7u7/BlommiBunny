package com.bloomibunny.security;
import com.bloomibunny.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import java.util.List;
@Configuration public class SecurityConfig {
 private final JwtAuthFilter filter; @Value("${app.cors.allowed-origin}") private String origin;
 public SecurityConfig(JwtAuthFilter filter){this.filter=filter;}
 @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception{
  return http.csrf(c->c.disable()).cors(c->c.configurationSource(cors()))
   .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->res.setStatus(401)).accessDeniedHandler((req,res,ex)->res.setStatus(403)))
   .authorizeHttpRequests(a->a
    .requestMatchers(HttpMethod.POST,"/api/auth/login","/api/auth/signup").permitAll()
    .requestMatchers(HttpMethod.GET,"/api/store/products","/api/store/products/*").permitAll()
    .requestMatchers("/api/favorites", "/api/favorites/*").authenticated()
    .requestMatchers("/api/**").hasRole("ADMIN")
    .anyRequest().denyAll())
   .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class).build();
 }
 @Bean PasswordEncoder encoder(){return new BCryptPasswordEncoder();}
 @Bean AuthenticationManager manager(AuthenticationConfiguration c)throws Exception{return c.getAuthenticationManager();}
 @Bean UserDetailsService users(AppUserRepository r){return n->r.findByUsername(n).filter(u->u.isActive()).map(u->User.builder().username(u.getUsername()).password(u.getPassword()).roles(u.getRole().name()).build()).orElseThrow(()->new UsernameNotFoundException(n));}
 @Bean CorsConfigurationSource cors(){var c=new CorsConfiguration();c.setAllowedOrigins(List.of(origin));c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));c.setAllowedHeaders(List.of("*"));c.setAllowCredentials(true);var s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;}
}
