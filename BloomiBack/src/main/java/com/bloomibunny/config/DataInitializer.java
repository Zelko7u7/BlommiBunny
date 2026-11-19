package com.bloomibunny.config;
import com.bloomibunny.entity.*;
import com.bloomibunny.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
@Component
public class DataInitializer implements CommandLineRunner {
 private static final Logger log=LoggerFactory.getLogger(DataInitializer.class);
 private final AppUserRepository users; private final CategoryRepository categories; private final PasswordEncoder encoder; private final JdbcTemplate jdbc;
 @Value("${app.bootstrap.admin-username}") String username; @Value("${app.bootstrap.admin-password}") String password;
 public DataInitializer(AppUserRepository u,CategoryRepository c,PasswordEncoder e,JdbcTemplate j){users=u;categories=c;encoder=e;jdbc=j;}
 @Transactional
 public void run(String...args){
  // Serialize first installation across application instances sharing this database.
  jdbc.queryForObject("select 1 from pg_advisory_xact_lock(?)",Integer.class,721604118L);
  long admins=users.countByRole(Role.ADMIN);
  if(admins>1)throw new IllegalStateException("Existe más de una administradora. Revisa las cuentas antes de iniciar; no se modificarán automáticamente.");
  if(admins==0){
   if(users.count()==0){
    if(username==null||username.isBlank()||password==null||password.length()<8||password.length()>100)throw new IllegalStateException("Configura las credenciales privadas de la administradora inicial (contraseña de 8 a 100 caracteres).");
    var u=new AppUser();u.setUsername(username.trim());u.setPassword(encoder.encode(password));u.setRole(Role.ADMIN);users.saveAndFlush(u);
   }else log.warn("Hay cuentas existentes sin administradora. Se requiere recuperación manual; no se crean ni ascienden cuentas automáticamente.");
  }
  if(categories.count()==0)List.of("Labiales","Bases","Correctores","Rubores","Brochas","Sombras","Delineadores","Pestañinas","Skincare").forEach(n->{var c=new Category();c.setName(n);categories.save(c);});
 }
}
