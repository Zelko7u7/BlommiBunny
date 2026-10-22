package com.bloomibunny.dto;
import com.bloomibunny.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
public final class TransactionDtos {
 private TransactionDtos(){}
 public record LineRequest(@NotNull Long productId,@Min(1) int quantity,@NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal unitCost){}
 public record PurchaseRequest(@NotNull UUID requestId,@NotNull Long supplierId,@NotNull LocalDate date,@Size(max=120) String reference,@Size(max=1000) String notes,@NotEmpty @Size(max=100) List<@NotNull @Valid LineRequest> items){}
 public record LineResponse(Long productId,String product,String code,String shade,int quantity,BigDecimal unitCost,BigDecimal subtotal){}
 public record PurchaseResponse(Long id,String supplier,LocalDate date,Instant registeredAt,String reference,String notes,BigDecimal total,String recordedBy,boolean cancelled,Instant cancelledAt,String cancellationReason,String cancelledBy,boolean historicalSnapshot,List<LineResponse> items){}
 public record ExpenseRequest(@NotNull UUID requestId,@NotBlank @Size(max=80) String category,@NotBlank @Size(max=180) String description,@NotNull @DecimalMin("0.01") @Digits(integer=12,fraction=2) BigDecimal amount,@NotNull LocalDate date,@NotNull PaymentMethod paymentMethod,@Size(max=120) String reference,@Size(max=1000) String notes){}
 public record ExpenseResponse(Long id,String category,String description,BigDecimal amount,LocalDate date,PaymentMethod paymentMethod,String reference,String notes,String recordedBy,boolean cancelled,Instant cancelledAt,String cancellationReason,String cancelledBy){}
 public record CancelRequest(@NotBlank @Size(min=5,max=500) String reason){}
 public interface Totals { BigDecimal getConfirmedTotal(); long getConfirmedCount(); long getCancelledCount(); }
 public record History<T>(List<T> content,long totalElements,int totalPages,int number,BigDecimal confirmedTotal,long confirmedCount,long cancelledCount){}
}
