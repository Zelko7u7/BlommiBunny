package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
@Entity @Table(indexes=@Index(name="idx_purchase_date",columnList="purchaseDate"))
@Getter @Setter @NoArgsConstructor
public class Purchase extends BaseEntity {
 @ManyToOne(optional=false) private Supplier supplier;
 @Column(nullable=false) private Instant purchaseDate=Instant.now();
 @Column(nullable=false,precision=14,scale=2) private BigDecimal total=BigDecimal.ZERO;
 @OneToMany(mappedBy="purchase",cascade=CascadeType.ALL,orphanRemoval=true) private List<PurchaseItem> items=new ArrayList<>();
}
