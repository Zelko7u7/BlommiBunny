package com.bloomibunny.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public final class ProductDtos {
 private ProductDtos(){}
 public record ProductRequest(@NotBlank @Size(max=140) String name,@NotBlank @Size(max=60) String code,@NotNull Long categoryId,Long brandId,Long supplierId,@Size(max=1200) String description,@Size(max=80) String shade,@Size(max=80) String presentation,@NotNull @DecimalMin("0") BigDecimal purchasePrice,@NotNull @DecimalMin("0") BigDecimal salePrice,@Min(0) int stock,@Min(0) int minimumStock,LocalDate entryDate,Boolean active,@Size(max=500) String imageUrl,Boolean provisionalData,Boolean costConfirmed,@Min(0) Long expectedVersion,@Size(max=80) String variantGroup){}
 public record ProductResponse(Long id,String name,String code,Long categoryId,String category,Long brandId,String brand,Long supplierId,String supplier,String description,String shade,String presentation,BigDecimal purchasePrice,BigDecimal salePrice,BigDecimal profit,int stock,int minimumStock,LocalDate entryDate,boolean active,String imageUrl,String stockStatus,boolean provisionalData,boolean costConfirmed,long version,String variantGroup){}
 public record StockAdjustment(@NotNull Integer quantity,@NotBlank String reason){}
 public record ProductStatus(@NotNull Boolean active){}
}
