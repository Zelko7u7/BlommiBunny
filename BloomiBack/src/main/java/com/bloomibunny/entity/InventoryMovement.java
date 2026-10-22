package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(indexes={@Index(name="idx_movement_product",columnList="product_id"),@Index(name="idx_movement_created",columnList="createdAt")})
@Getter @Setter @NoArgsConstructor
public class InventoryMovement extends BaseEntity {
 @ManyToOne(optional=false) private Product product;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private MovementType type;
 @Column(nullable=false) private int quantity;
 @Column(nullable=false) private int resultingStock;
 @Column(length=300) private String reference;
}
