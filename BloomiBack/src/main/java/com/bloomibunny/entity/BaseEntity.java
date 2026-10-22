package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.Getter;
import java.time.Instant;
@MappedSuperclass @Getter
public abstract class BaseEntity {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) protected Long id;
  @Column(nullable=false,updatable=false) protected Instant createdAt;
  @Column(nullable=false) protected Instant updatedAt;
  @PrePersist void create(){createdAt=updatedAt=Instant.now();}
  @PreUpdate void update(){updatedAt=Instant.now();}
}
