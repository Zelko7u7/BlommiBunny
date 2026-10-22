package com.bloomibunny.service;

import com.bloomibunny.repository.*;
import com.bloomibunny.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
 @Mock DashboardRepository data;
 @Mock ProductRepository products;
 @Mock CustomerRepository customers;
 @Mock InventoryMovementRepository movements;
 DashboardServiceImpl service;
 @BeforeEach void setup(){
  service=new DashboardServiceImpl(data,products,customers,movements,Clock.fixed(Instant.parse("2026-11-01T02:30:00Z"),ZoneOffset.UTC));
  when(data.revenue(any(),any())).thenReturn(revenue("0",0));when(data.costs(any(),any())).thenReturn(costs("0",0,0));
 }
 DashboardRepository.Revenue revenue(String total,long count){return new DashboardRepository.Revenue(){public BigDecimal getTotal(){return new BigDecimal(total);}public long getCount(){return count;}};}
 DashboardRepository.Costs costs(String known,long sales,long units){return new DashboardRepository.Costs(){public BigDecimal getKnownCost(){return new BigDecimal(known);}public long getUnknownSales(){return sales;}public long getUnknownUnits(){return units;}};}
 @Test void emptyDashboardContainsRealZeroesAndContinuousPeriods(){
  var result=service.summary();assertEquals(0,result.salesMonth().signum());assertEquals(0,result.costMonth().signum());assertEquals(0,result.grossProfit().signum());assertEquals(30,result.dailySales().size());assertEquals(7,result.monthlySales().size());assertTrue(result.dailySales().stream().allMatch(point->point.total().signum()==0&&point.count()==0));assertTrue(result.topProducts().isEmpty());
 }
 @Test void boundariesUseBogotaInsteadOfUtc(){
  var result=service.summary();assertEquals(LocalDate.of(2026,10,31),result.today());assertEquals("America/Bogota",result.timezone());
  var end=Instant.parse("2026-11-01T05:00:00Z");verify(data).revenue(Instant.parse("2026-10-31T05:00:00Z"),end);verify(data).revenue(Instant.parse("2026-10-01T05:00:00Z"),end);verify(data).daily(Instant.parse("2026-10-02T05:00:00Z"),end);verify(data).monthly(Instant.parse("2026-04-01T05:00:00Z"),end);
 }
 @Test void marginUsesHistoricalCostRatherThanExpenses(){
  when(data.revenue(any(),any())).thenReturn(revenue("19000",1));when(data.costs(any(),any())).thenReturn(costs("8000",0,0));var result=service.summary();assertEquals(new BigDecimal("11000"),result.grossProfit());assertEquals(new BigDecimal("8000"),result.costMonth());
 }
 @Test void missingHistoricalCostsDoNotBecomeZeroCostOrFakeMargin(){
  when(data.costs(any(),any())).thenReturn(costs("5000",2,3));var result=service.summary();assertNull(result.costMonth());assertNull(result.grossProfit());assertEquals(new BigDecimal("5000"),result.knownCostMonth());assertEquals(2,result.unknownCostSales());assertEquals(3,result.unknownCostUnits());
 }
 @Test void periodGapsAreFilledWithoutInventingSales(){
  var day=mock(DashboardRepository.Period.class);when(day.getDate()).thenReturn("2026-10-30");when(day.getTotal()).thenReturn(new BigDecimal("4500"));when(day.getCount()).thenReturn(1L);when(data.daily(any(),any())).thenReturn(List.of(day));
  var result=service.summary();assertEquals(new BigDecimal("4500"),result.dailySales().get(28).total());assertEquals(0,result.dailySales().getLast().count());assertEquals(LocalDate.of(2026,10,31),result.dailySales().getLast().date());
 }
}
