package com.bloomibunny.repository;
import com.bloomibunny.entity.Purchase;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import com.bloomibunny.dto.TransactionDtos.Totals;
import java.time.LocalDate;
import java.util.*;
public interface PurchaseRepository extends JpaRepository<Purchase,Long>{
 Optional<Purchase> findByRequestId(UUID requestId);
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @Query("select p from Purchase p where p.id=:id") Optional<Purchase> lockById(@Param("id") Long id);
 String FILTER="""
  from Purchase p where (cast(:from as date) is null or p.receivedOn>=:from)
  and (cast(:to as date) is null or p.receivedOn<=:to)
  and (cast(:cancelled as boolean) is null or p.cancelled=:cancelled)
  and (:q='' or cast(p.id as string)=:q
   or lower(coalesce(p.supplierName,p.supplier.name)) like lower(concat('%',:q,'%'))
   or lower(coalesce(p.reference,'')) like lower(concat('%',:q,'%'))
   or exists(select i.id from PurchaseItem i where i.purchase=p and
    (lower(coalesce(i.productName,i.product.name)) like lower(concat('%',:q,'%'))
     or lower(coalesce(i.productCode,i.product.code)) like lower(concat('%',:q,'%'))
     or lower(coalesce(i.shade,i.product.shade,'')) like lower(concat('%',:q,'%')))))
  """;
 @Query("select p "+FILTER)
 Page<Purchase> search(@Param("q") String q,@Param("from") LocalDate from,@Param("to") LocalDate to,@Param("cancelled") Boolean cancelled,Pageable pageable);
 @Query("select coalesce(sum(case when p.cancelled=false then p.total else 0 end),0) as confirmedTotal,count(case when p.cancelled=false then p.id else null end) as confirmedCount,count(case when p.cancelled=true then p.id else null end) as cancelledCount "+FILTER)
 Totals totals(@Param("q") String q,@Param("from") LocalDate from,@Param("to") LocalDate to,@Param("cancelled") Boolean cancelled);
}
