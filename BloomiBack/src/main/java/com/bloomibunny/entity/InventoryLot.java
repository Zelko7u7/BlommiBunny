package com.bloomibunny.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Getter @Setter @NoArgsConstructor
@Table(uniqueConstraints=@UniqueConstraint(name="uk_lot_product_source",columnNames={"product_id","source_key"}))
public class InventoryLot extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private Product product;
 @Column(nullable=false,length=100) private String sourceKey;
 @Column(nullable=false,length=160) private String reference;
 @Column(nullable=false) private boolean opening;
 @Column(nullable=false) private LocalDate receivedOn;
 @Column(nullable=false) private Instant receivedAt;
 @Column(nullable=false) private int remaining;
 @Column(precision=14,scale=2) private BigDecimal unitCost;
}
