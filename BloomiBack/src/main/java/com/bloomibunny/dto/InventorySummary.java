package com.bloomibunny.dto;

import java.math.BigDecimal;

public record InventorySummary(long totalProducts,long confirmedUnits,BigDecimal saleValue,
 BigDecimal confirmedCostValue,long unknownCostProducts,long unknownCostUnits,
 long provisionalProducts,long provisionalUnits) {}
