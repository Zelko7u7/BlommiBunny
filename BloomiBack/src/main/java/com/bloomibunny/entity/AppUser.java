package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="app_user",indexes=@Index(name="idx_user_username",columnList="username"))
@Getter @Setter @NoArgsConstructor
public class AppUser extends BaseEntity {
 @Column(nullable=false,unique=true,length=80) private String username;
 @Column(nullable=false) private String password;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Role role=Role.CUSTOMER;
 @Column(nullable=false) private boolean active=true;
}
