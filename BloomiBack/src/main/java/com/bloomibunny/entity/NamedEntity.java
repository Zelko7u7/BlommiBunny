package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
@MappedSuperclass @Getter @Setter
public abstract class NamedEntity extends BaseEntity {
 @Column(nullable=false,unique=true,length=120) protected String name;
 @Column(length=500) protected String description;
 @Column(nullable=false) protected boolean active=true;
}
