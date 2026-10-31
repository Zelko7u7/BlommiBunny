package com.bloomibunny.dto;

import com.bloomibunny.entity.PaymentMethod;
import com.bloomibunny.entity.SaleChannel;
import com.bloomibunny.entity.InitialPaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class SaleDtos {
 private SaleDtos() {}
 public record SaleLineRequest(@NotNull Long productId, @Min(1) int quantity,
  @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal unitPrice) {}
 public record CreateSaleRequest(@NotNull UUID requestId, Long customerId, @NotNull PaymentMethod paymentMethod,
  @NotNull SaleChannel channel, @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal discount,
  @Size(max=500) String notes, @NotEmpty @Size(max=100) List<@NotNull @Valid SaleLineRequest> items,
  InitialPaymentStatus paymentStatus,@DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal initialPayment) {
  public CreateSaleRequest(UUID requestId,Long customerId,PaymentMethod paymentMethod,SaleChannel channel,BigDecimal discount,String notes,List<SaleLineRequest> items){
   this(requestId,customerId,paymentMethod,channel,discount,notes,items,null,null);
  }
 }
 public record CancelSaleRequest(@NotBlank @Size(min=5,max=500) String reason) {}
 public record PaymentRequest(@NotNull UUID requestId,
  @NotNull @DecimalMin("0.01") @Digits(integer=12,fraction=2) BigDecimal amount,
  @NotNull LocalDate date,@NotNull PaymentMethod paymentMethod,
  @Size(max=120) String reference,@Size(max=500) String notes) {}
 public record PaymentResponse(Long id,BigDecimal amount,LocalDate date,PaymentMethod paymentMethod,
  String reference,String notes,String recordedBy,Instant recordedAt,
  boolean cancelled,Instant cancelledAt,String cancellationReason,String cancelledBy) {}
 public record CollectionRequest(@NotNull @Min(0) Long expectedVersion,boolean authorized,
  @NotBlank @Size(min=5,max=500) String reason) {}
 public record ReviewPaymentsRequest(@NotNull @Min(0) Long expectedVersion,
  @NotBlank @Size(min=5,max=500) String reason) {}
 public record CollectionEventResponse(Long id,String action,String reason,String recordedBy,Instant date) {}
 public record SaleLine(Long productId, String product, String code, String shade, int quantity,
  BigDecimal unitPrice, BigDecimal unitCost, BigDecimal subtotal) {}
 public record SaleResponse(Long id, String customer, Instant date, PaymentMethod paymentMethod, SaleChannel channel,
  BigDecimal subtotal, BigDecimal discount, BigDecimal total, BigDecimal costTotal, BigDecimal grossProfit,
  String notes, String recordedBy, boolean cancelled, Instant cancelledAt, String cancellationReason,
  String cancelledBy, boolean historicalSnapshot, List<SaleLine> items,
  BigDecimal paidTotal,BigDecimal balance,String paymentStatus,boolean paymentsReviewed,
  List<PaymentResponse> payments,long version,boolean collectionAuthorized,Instant collectionAuthorizedAt,
  String collectionAuthorizedBy,List<CollectionEventResponse> collectionEvents) {}
}
