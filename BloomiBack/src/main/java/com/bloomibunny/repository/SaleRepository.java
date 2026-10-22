package com.bloomibunny.repository;
import com.bloomibunny.entity.Sale;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public interface SaleRepository extends JpaRepository<Sale,Long>{
 List<Sale> findBySaleDateBetweenOrderBySaleDateDesc(Instant from,Instant to);
 @Query("select coalesce(sum(s.total),0) from Sale s where s.saleDate between :from and :to") BigDecimal total(@Param("from") Instant from,@Param("to") Instant to);
}
