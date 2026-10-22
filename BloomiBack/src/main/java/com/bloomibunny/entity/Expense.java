package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(indexes=@Index(name="idx_expense_date",columnList="expenseDate"))
@Getter @Setter @NoArgsConstructor
public class Expense extends BaseEntity {
 @Column(nullable=false,length=80) private String category;
 @Column(nullable=false,length=180) private String description;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal amount;
 @Column(nullable=false) private LocalDate expenseDate=LocalDate.now();
}
