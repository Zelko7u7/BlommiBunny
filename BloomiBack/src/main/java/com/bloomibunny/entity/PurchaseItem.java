package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name="purchase_item") @Getter @Setter @NoArgsConstructor
public class PurchaseItem extends BaseEntity {
 @ManyToOne(optional=false) private Purchase purchase;
 @ManyToOne(optional=false) private Product product;
 @Column(nullable=false) private int quantity;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal unitCost;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal subtotal;
}
