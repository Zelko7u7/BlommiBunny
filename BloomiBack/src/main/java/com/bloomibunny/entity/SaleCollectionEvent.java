package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor
public class SaleCollectionEvent extends BaseEntity {
 @ManyToOne(optional=false) @JoinColumn(nullable=false) private Sale sale;
 @Column(nullable=false,length=20) private String action;
 @Column(nullable=false,length=500) private String reason;
 @Column(nullable=false,length=80) private String recordedBy;
}
