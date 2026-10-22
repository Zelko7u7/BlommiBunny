package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name="sale_item") @Getter @Setter @NoArgsConstructor
public class SaleItem extends BaseEntity {
 @ManyToOne(optional=false) private Sale sale;
 @ManyToOne(optional=false) private Product product;
 @Column(nullable=false) private int quantity;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal unitPrice;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal subtotal;
}
