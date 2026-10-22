package com.bloomibunny.dto;
import com.bloomibunny.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
public final class TransactionDtos {
 private TransactionDtos(){}
 public record LineRequest(@NotNull Long productId,@Min(1) int quantity,@DecimalMin("0") BigDecimal unitPrice){}
 public record PurchaseRequest(@NotNull Long supplierId,@NotEmpty List<@Valid LineRequest> items){}
 public record LineResponse(Long productId,String product,int quantity,BigDecimal unitPrice,BigDecimal subtotal){}
 public record PurchaseResponse(Long id,String supplier,Instant date,BigDecimal total,List<LineResponse> items){}
 public record ExpenseRequest(@NotBlank String category,@NotBlank String description,@NotNull @DecimalMin("0.01") BigDecimal amount,LocalDate expenseDate){}
}
