package com.bloomibunny.repository;
import com.bloomibunny.entity.Expense;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
public interface ExpenseRepository extends JpaRepository<Expense,Long>{
 @Query("select coalesce(sum(e.amount),0) from Expense e where e.expenseDate between :from and :to") BigDecimal total(@Param("from") LocalDate from,@Param("to") LocalDate to);
}
