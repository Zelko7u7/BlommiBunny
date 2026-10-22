package com.bloomibunny.service.impl;
import com.bloomibunny.dto.DashboardResponse;
import com.bloomibunny.repository.*;
import com.bloomibunny.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
@Service @Transactional(readOnly=true)
public class DashboardServiceImpl implements DashboardService {
 private final SaleRepository sales;private final ProductRepository products;private final ExpenseRepository expenses;private final CustomerRepository customers;private final InventoryMovementRepository movements;
 public DashboardServiceImpl(SaleRepository s,ProductRepository p,ExpenseRepository e,CustomerRepository c,InventoryMovementRepository m){sales=s;products=p;expenses=e;customers=c;movements=m;}
 public DashboardResponse summary(){var today=LocalDate.now(ZoneOffset.UTC);var dayStart=today.atStartOfDay().toInstant(ZoneOffset.UTC);var dayEnd=today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);var month=today.withDayOfMonth(1);var monthStart=month.atStartOfDay().toInstant(ZoneOffset.UTC);BigDecimal monthSales=sales.total(monthStart,dayEnd);BigDecimal monthExpenses=expenses.total(month,today);long low=products.findAll().stream().filter(p->!p.isDeleted()&&p.getStock()>0&&p.getStock()<=p.getMinimumStock()).count();var recent=movements.findTop10ByOrderByCreatedAtDesc().stream().map(m->new DashboardResponse.Movement(m.getProduct().getName(),m.getType().name(),m.getQuantity(),m.getResultingStock(),m.getCreatedAt().toString())).toList();return new DashboardResponse(sales.total(dayStart,dayEnd),monthSales,low,products.countByStockAndDeletedFalse(0),products.countByDeletedFalse(),monthSales.subtract(monthExpenses),monthExpenses,customers.count(),recent);}
}
