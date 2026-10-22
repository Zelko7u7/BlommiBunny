package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name="sale_item") @Getter @Setter @NoArgsConstructor
public class SaleItem extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private Sale sale;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private Product product;
 @Column(nullable=false) private int quantity;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal unitPrice;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal subtotal;
 @Column(length=140) private String productName;
 @Column(length=60) private String productCode;
 @Column(length=80) private String shade;
 @Column(precision=14,scale=2) private BigDecimal unitCost;
}
