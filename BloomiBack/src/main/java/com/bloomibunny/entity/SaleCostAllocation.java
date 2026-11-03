package com.bloomibunny.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

/** Historical cost snapshot, independent of the disposable live lot row. */
@Embeddable @Getter @Setter @NoArgsConstructor
public class SaleCostAllocation {
 @Column(nullable=false,length=100) private String sourceKey;
 @Column(nullable=false,length=160) private String reference;
 @Column(nullable=false) private boolean opening;
 @Column(nullable=false) private LocalDate receivedOn;
 @Column(nullable=false) private Instant receivedAt;
 @Column(nullable=false) private int quantity;
 @Column(precision=14,scale=2) private BigDecimal unitCost;
}
