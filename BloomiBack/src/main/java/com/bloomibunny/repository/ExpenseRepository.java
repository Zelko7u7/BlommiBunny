package com.bloomibunny.repository;
import com.bloomibunny.entity.Expense;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.*;
import com.bloomibunny.dto.TransactionDtos.Totals;
import java.util.*;
public interface ExpenseRepository extends JpaRepository<Expense,Long>{
 Optional<Expense> findByRequestId(UUID requestId);
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @Query("select e from Expense e where e.id=:id") Optional<Expense> lockById(@Param("id") Long id);
 String FILTER="""
  from Expense e where (cast(:from as date) is null or e.expenseDate>=:from)
  and (cast(:to as date) is null or e.expenseDate<=:to)
  and (cast(:cancelled as boolean) is null or e.cancelled=:cancelled)
  and (:q='' or cast(e.id as string)=:q or lower(e.category) like lower(concat('%',:q,'%'))
   or lower(e.description) like lower(concat('%',:q,'%'))
   or lower(coalesce(e.reference,'')) like lower(concat('%',:q,'%')))
  """;
 @Query("select e "+FILTER)
 Page<Expense> search(@Param("q") String q,@Param("from") LocalDate from,@Param("to") LocalDate to,@Param("cancelled") Boolean cancelled,Pageable pageable);
 @Query("select coalesce(sum(case when e.cancelled=false then e.amount else 0 end),0) as confirmedTotal,count(case when e.cancelled=false then e.id else null end) as confirmedCount,count(case when e.cancelled=true then e.id else null end) as cancelledCount "+FILTER)
 Totals totals(@Param("q") String q,@Param("from") LocalDate from,@Param("to") LocalDate to,@Param("cancelled") Boolean cancelled);
 @Query("select coalesce(sum(e.amount),0) from Expense e where e.cancelled=false and e.expenseDate between :from and :to") BigDecimal total(@Param("from") LocalDate from,@Param("to") LocalDate to);
}
