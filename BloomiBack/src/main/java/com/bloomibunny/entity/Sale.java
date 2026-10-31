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
 @Column(unique=true) private UUID requestId;
 @Column(length=64) private String requestFingerprint;
 @Column(length=140) private String customerName;
 @Enumerated(EnumType.STRING) @Column(length=20) private SaleChannel channel;
 @Column(length=500) private String notes;
 @Column(length=80) private String recordedBy;
 @Column(nullable=false,columnDefinition="boolean default false") private boolean cancelled=false;
 private Instant cancelledAt;
 @Column(length=500) private String cancellationReason;
 @Column(length=80) private String cancelledBy;
 @Version @Column(nullable=false,columnDefinition="bigint default 0") private long version;
 // Historical sales are explicitly left unreviewed by migration 010.
 @Column(nullable=false,columnDefinition="boolean default false") private boolean paymentsReviewed=true;
 @Column(nullable=false,columnDefinition="boolean default false") private boolean collectionAuthorized=false;
 private Instant collectionAuthorizedAt;
 @Column(length=80) private String collectionAuthorizedBy;
 private Instant paymentsChangedAt;
 private Instant paymentsReviewedAt;
 @Column(length=80) private String paymentsReviewedBy;
 @OneToMany(mappedBy="sale",cascade=CascadeType.ALL,orphanRemoval=true) private List<SaleItem> items=new ArrayList<>();
}
