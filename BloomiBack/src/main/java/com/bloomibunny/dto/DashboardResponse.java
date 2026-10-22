package com.bloomibunny.dto;
import java.math.BigDecimal;
import java.util.List;
public record DashboardResponse(BigDecimal salesToday,BigDecimal salesMonth,long lowStock,long outOfStock,long totalProducts,BigDecimal grossProfit,BigDecimal expenses,long customers,List<Movement> recentMovements){
 public record Movement(String product,String type,int quantity,int resultingStock,String date){}
}
