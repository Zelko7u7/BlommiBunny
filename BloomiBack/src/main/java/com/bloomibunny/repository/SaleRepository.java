package com.bloomibunny.repository;
import com.bloomibunny.entity.Sale;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public interface SaleRepository extends JpaRepository<Sale,Long>{
 java.util.Optional<Sale> findByRequestId(java.util.UUID requestId);
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @Query("select s from Sale s where s.id=:id") java.util.Optional<Sale> lockById(@Param("id") Long id);
 @Query("""
  select s from Sale s left join s.customer c where
  (cast(:from as timestamp) is null or s.saleDate>=:from) and (cast(:to as timestamp) is null or s.saleDate<:to)
  and (cast(:cancelled as boolean) is null or s.cancelled=:cancelled) and (cast(:channel as string) is null or s.channel=:channel)
  and (:q='' or cast(s.id as string)=:q
    or lower(coalesce(s.customerName,c.name,'Consumidor final')) like lower(concat('%',:q,'%'))
    or exists(select i.id from SaleItem i where i.sale=s and
      (lower(coalesce(i.productName,i.product.name)) like lower(concat('%',:q,'%'))
       or lower(coalesce(i.productCode,i.product.code)) like lower(concat('%',:q,'%'))
       or lower(coalesce(i.shade,i.product.shade,'')) like lower(concat('%',:q,'%')))))
  """)
 org.springframework.data.domain.Page<Sale> search(@Param("q") String q,@Param("from") Instant from,
  @Param("to") Instant to,@Param("cancelled") Boolean cancelled,@Param("channel") com.bloomibunny.entity.SaleChannel channel,
  org.springframework.data.domain.Pageable pageable);
 List<Sale> findBySaleDateBetweenOrderBySaleDateDesc(Instant from,Instant to);
 @Query("select coalesce(sum(s.total),0) from Sale s where s.cancelled=false and s.saleDate>=:from and s.saleDate<:to") BigDecimal total(@Param("from") Instant from,@Param("to") Instant to);
}
