package com.bloomibunny.service.impl;
import com.bloomibunny.dto.DashboardResponse;
import com.bloomibunny.dto.DashboardResponse.*;
import com.bloomibunny.repository.*;
import com.bloomibunny.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Service @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
public class DashboardServiceImpl implements DashboardService {
 private static final ZoneId ZONE=ZoneId.of("America/Bogota");
 private final DashboardRepository data;
 private final ProductRepository products;
 private final CustomerRepository customers;
 private final InventoryMovementRepository movements;
 private final Clock clock;
 public DashboardServiceImpl(DashboardRepository data,ProductRepository products,CustomerRepository customers,InventoryMovementRepository movements,Clock clock){this.data=data;this.products=products;this.customers=customers;this.movements=movements;this.clock=clock;}

 public DashboardResponse summary(){
  var now=clock.instant();var today=now.atZone(ZONE).toLocalDate();var month=today.withDayOfMonth(1);
  var end=start(today.plusDays(1));var todayRevenue=data.revenue(start(today),end);var monthRevenue=data.revenue(start(month),end);
  var costs=data.costs(start(month),end);boolean complete=costs.getUnknownSales()==0;
  var daily=fill(data.daily(start(today.minusDays(29)),end),today.minusDays(29),30,false);
  var monthly=fill(data.monthly(start(month.minusMonths(6)),end),month.minusMonths(6),7,true);
  var top=data.bestSellers(start(month),end).stream().map(item->new TopProduct(item.getId(),item.getProduct(),item.getCode(),item.getShade(),item.getUnits())).toList();
  var alerts=products.alerts(org.springframework.data.domain.PageRequest.of(0,8)).stream().map(product->new StockAlert(product.getId(),product.getName(),product.getCode(),product.getShade(),product.getStock(),product.getMinimumStock(),product.isActive())).toList();
  var recent=movements.findTop10ByOrderByCreatedAtDesc().stream().map(m->{var product=m.getProduct();return new Movement(product.getId(),product.getName(),product.getCode(),product.getShade(),product.isDeleted(),m.getType().name(),m.getQuantity(),m.getResultingStock(),m.getReference(),m.getCreatedAt());}).toList();
  return new DashboardResponse(today,ZONE.getId(),now,todayRevenue.getTotal(),todayRevenue.getCount(),monthRevenue.getTotal(),monthRevenue.getCount(),complete?costs.getKnownCost():null,costs.getKnownCost(),complete?monthRevenue.getTotal().subtract(costs.getKnownCost()):null,costs.getUnknownSales(),costs.getUnknownUnits(),products.countLowStock(),products.countByStockAndDeletedFalse(0),products.countByDeletedFalse(),products.countByActiveTrueAndDeletedFalse(),customers.count(),daily,monthly,top,alerts,recent,
   zero(data.authorizedCollections(start(today),end)),zero(data.authorizedCollections(start(month),end)),
   zero(data.recordedPayments(month,today.plusDays(1))),zero(data.outstandingBalance()),data.unreviewedPayments());
 }
 private Instant start(LocalDate date){return date.atStartOfDay(ZONE).toInstant();}
 private BigDecimal zero(BigDecimal value){return value==null?BigDecimal.ZERO:value;}
 private List<PeriodSales> fill(List<DashboardRepository.Period> rows,LocalDate first,int count,boolean months){
  var grouped=new HashMap<LocalDate,PeriodSales>();
  for(var row:rows){var date=LocalDate.parse(row.getDate());grouped.put(date,new PeriodSales(date,row.getTotal(),row.getCount()));}
  var result=new ArrayList<PeriodSales>();
  for(int i=0;i<count;i++){var date=months?first.plusMonths(i):first.plusDays(i);result.add(grouped.getOrDefault(date,new PeriodSales(date,BigDecimal.ZERO,0)));}
  return result;
 }
}
