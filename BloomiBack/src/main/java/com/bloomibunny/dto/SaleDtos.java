package com.bloomibunny.dto;

import com.bloomibunny.entity.PaymentMethod;
import com.bloomibunny.entity.SaleChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SaleDtos {
 private SaleDtos() {}
 public record SaleLineRequest(@NotNull Long productId, @Min(1) int quantity,
  @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal unitPrice) {}
 public record CreateSaleRequest(@NotNull UUID requestId, Long customerId, @NotNull PaymentMethod paymentMethod,
  @NotNull SaleChannel channel, @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal discount,
  @Size(max=500) String notes, @NotEmpty @Size(max=100) List<@NotNull @Valid SaleLineRequest> items) {}
 public record CancelSaleRequest(@NotBlank @Size(min=5,max=500) String reason) {}
 public record SaleLine(Long productId, String product, String code, String shade, int quantity,
  BigDecimal unitPrice, BigDecimal unitCost, BigDecimal subtotal) {}
 public record SaleResponse(Long id, String customer, Instant date, PaymentMethod paymentMethod, SaleChannel channel,
  BigDecimal subtotal, BigDecimal discount, BigDecimal total, BigDecimal costTotal, BigDecimal grossProfit,
  String notes, String recordedBy, boolean cancelled, Instant cancelledAt, String cancellationReason,
  String cancelledBy, boolean historicalSnapshot, List<SaleLine> items) {}
}
