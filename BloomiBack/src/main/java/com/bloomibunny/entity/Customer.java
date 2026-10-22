package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
@Entity @Table(indexes=@Index(name="idx_customer_name",columnList="name"))
@Getter @Setter
public class Customer extends Contact { private LocalDate birthday; }
