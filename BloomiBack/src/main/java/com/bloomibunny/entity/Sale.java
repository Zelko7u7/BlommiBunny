package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
@Entity @Table(indexes=@Index(name="idx_sale_date",columnList="saleDate"))
@Getter @Setter @NoArgsConstructor
public class Sale extends BaseEntity {
 @ManyToOne private Customer customer;
 @Column(nullable=false) private Instant saleDate=Instant.now();
 @Enumerated(EnumType.STRING) @Column(nullable=false) private PaymentMethod paymentMethod;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal subtotal;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal discount=BigDecimal.ZERO;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal total;
 @OneToMany(mappedBy="sale",cascade=CascadeType.ALL,orphanRemoval=true) private List<SaleItem> items=new ArrayList<>();
}
