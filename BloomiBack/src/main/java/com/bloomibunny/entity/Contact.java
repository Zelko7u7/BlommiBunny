package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
@MappedSuperclass @Getter @Setter
public abstract class Contact extends BaseEntity {
 @Column(nullable=false,length=140) protected String name;
 @Column(length=30) protected String phone;
 @Column(length=150) protected String email;
 @Column(length=240) protected String address;
 @Column(length=1000) protected String notes;
 @Column(nullable=false) protected boolean active=true;
}
