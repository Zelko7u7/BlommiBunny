package com.bloomibunny.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
public record DashboardResponse(LocalDate today,String timezone,Instant generatedAt,
 BigDecimal salesToday,long salesTodayCount,BigDecimal salesMonth,long salesMonthCount,
 BigDecimal costMonth,BigDecimal knownCostMonth,BigDecimal grossProfit,long unknownCostSales,long unknownCostUnits,
 long lowStock,long outOfStock,long totalProducts,long activeProducts,long customers,
 List<PeriodSales> dailySales,List<PeriodSales> monthlySales,List<TopProduct> topProducts,
 List<StockAlert> stockAlerts,List<Movement> recentMovements,
 BigDecimal authorizedCollectionsToday,BigDecimal authorizedCollectionsMonth,
 BigDecimal recordedPaymentsMonth,BigDecimal outstandingBalance,long unreviewedPayments){
 public record PeriodSales(LocalDate date,BigDecimal total,long count){}
 public record TopProduct(Long id,String product,String code,String shade,long units){}
 public record StockAlert(Long id,String product,String code,String shade,int stock,int minimumStock,boolean active){}
 public record Movement(Long productId,String product,String code,String shade,boolean deleted,String type,int quantity,int resultingStock,String reference,Instant date){}
}
