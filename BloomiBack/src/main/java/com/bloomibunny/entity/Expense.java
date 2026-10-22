package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(indexes=@Index(name="idx_expense_date",columnList="expenseDate"))
@Getter @Setter @NoArgsConstructor
public class Expense extends BaseEntity {
 @Column(nullable=false,length=80) private String category;
 @Column(nullable=false,length=180) private String description;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal amount;
 @Column(nullable=false) private LocalDate expenseDate=LocalDate.now();
 @Enumerated(EnumType.STRING) @Column(length=20) private PaymentMethod paymentMethod;
 @Column(length=120) private String reference;
 @Column(length=1000) private String notes;
 @Column(length=80) private String recordedBy;
 @Column(unique=true) private UUID requestId;
 @Column(length=64) private String requestFingerprint;
 @Column(nullable=false,columnDefinition="boolean default false") private boolean cancelled=false;
 private Instant cancelledAt;
 @Column(length=500) private String cancellationReason;
 @Column(length=80) private String cancelledBy;
}
