package com.bloomibunny.repository;
import com.bloomibunny.entity.AppUser;
import com.bloomibunny.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AppUserRepository extends JpaRepository<AppUser,Long>{Optional<AppUser> findByUsername(String username);long countByRole(Role role);}
