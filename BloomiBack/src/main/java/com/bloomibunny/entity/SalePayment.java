package com.bloomibunny.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity @Getter @Setter @NoArgsConstructor
@Table(indexes=@Index(name="idx_sale_payment_sale",columnList="sale_id"))
public class SalePayment extends BaseEntity {
 @ManyToOne(optional=false) @JoinColumn(nullable=false) private Sale sale;
 @Column(nullable=false,unique=true) private UUID requestId;
 @Column(nullable=false,length=64) private String requestFingerprint;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal amount;
 @Column(nullable=false) private LocalDate paymentDate;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PaymentMethod paymentMethod;
 @Column(length=120) private String reference;
 @Column(length=500) private String notes;
 @Column(nullable=false,length=80) private String recordedBy;
 @Column(nullable=false) private boolean cancelled=false;
 private Instant cancelledAt;
 @Column(length=500) private String cancellationReason;
 @Column(length=80) private String cancelledBy;
}
