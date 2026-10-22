package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
@Entity @Table(indexes=@Index(name="idx_purchase_date",columnList="purchaseDate"))
@Getter @Setter @NoArgsConstructor
public class Purchase extends BaseEntity {
 @ManyToOne(optional=false,fetch=FetchType.LAZY) private Supplier supplier;
 @Column(nullable=false) private Instant purchaseDate=Instant.now();
 @Column(nullable=false,precision=14,scale=2) private BigDecimal total=BigDecimal.ZERO;
 private LocalDate receivedOn;
 @Column(length=140) private String supplierName;
 @Column(length=120) private String reference;
 @Column(length=1000) private String notes;
 @Column(length=80) private String recordedBy;
 @Column(unique=true) private UUID requestId;
 @Column(length=64) private String requestFingerprint;
 @Column(nullable=false,columnDefinition="boolean default false") private boolean cancelled=false;
 private Instant cancelledAt;
 @Column(length=500) private String cancellationReason;
 @Column(length=80) private String cancelledBy;
 @OneToMany(mappedBy="purchase",cascade=CascadeType.ALL,orphanRemoval=true) private List<PurchaseItem> items=new ArrayList<>();
}
