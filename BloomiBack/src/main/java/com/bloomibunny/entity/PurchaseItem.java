package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name="purchase_item") @Getter @Setter @NoArgsConstructor
public class PurchaseItem extends BaseEntity {
 @ManyToOne(optional=false,fetch=FetchType.LAZY) private Purchase purchase;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) private Product product;
 @Column(length=140) private String productName;
 @Column(length=60) private String productCode;
 @Column(length=80) private String shade;
 @Column(nullable=false) private int quantity;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal unitCost;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal subtotal;
}
