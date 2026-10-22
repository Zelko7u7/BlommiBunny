package com.bloomibunny.config;
import com.bloomibunny.entity.*;
import com.bloomibunny.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
public class DataInitializer implements CommandLineRunner {
 private final AppUserRepository users; private final CategoryRepository categories; private final PasswordEncoder encoder;
 @Value("${app.bootstrap.admin-username}") String username; @Value("${app.bootstrap.admin-password}") String password;
 public DataInitializer(AppUserRepository u,CategoryRepository c,PasswordEncoder e){users=u;categories=c;encoder=e;}
 public void run(String...args){if(users.findByUsername(username).isEmpty()){var u=new AppUser();u.setUsername(username);u.setPassword(encoder.encode(password));u.setRole(Role.ADMIN);users.save(u);}if(categories.count()==0)List.of("Labiales","Bases","Correctores","Rubores","Brochas","Sombras","Delineadores","Pestañinas","Skincare").forEach(n->{var c=new Category();c.setName(n);categories.save(c);});}
}
