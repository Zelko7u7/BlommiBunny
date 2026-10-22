package com.bloomibunny.repository;

import com.bloomibunny.entity.SaleItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface DashboardRepository extends Repository<SaleItem,Long> {
 interface Revenue { BigDecimal getTotal(); long getCount(); }
 interface Costs { BigDecimal getKnownCost(); long getUnknownSales(); long getUnknownUnits(); }
 interface Period { String getDate(); BigDecimal getTotal(); long getCount(); }
 interface BestSeller { Long getId(); String getProduct(); String getCode(); String getShade(); long getUnits(); }

 @Query("select coalesce(sum(s.total),0) as total,count(s) as count from Sale s where s.cancelled=false and s.saleDate>=:from and s.saleDate<:to")
 Revenue revenue(@Param("from") Instant from,@Param("to") Instant to);

 @Query("""
  select coalesce(sum(case when i.unitCost is not null then i.unitCost*i.quantity else 0 end),0) as knownCost,
  count(distinct case when i.unitCost is null then s.id else null end) as unknownSales,
  coalesce(sum(case when i.unitCost is null then i.quantity else 0 end),0) as unknownUnits
  from SaleItem i join i.sale s where s.cancelled=false and s.saleDate>=:from and s.saleDate<:to
  """)
 Costs costs(@Param("from") Instant from,@Param("to") Instant to);

 @Query(value="""
  select to_char(s.sale_date at time zone 'America/Bogota','YYYY-MM-DD') as date,
  sum(s.total) as total,count(*) as count from sale s
  where s.cancelled=false and s.sale_date>=:from and s.sale_date<:to group by 1 order by 1
  """,nativeQuery=true)
 List<Period> daily(@Param("from") Instant from,@Param("to") Instant to);

 @Query(value="""
  select to_char(date_trunc('month',s.sale_date at time zone 'America/Bogota'),'YYYY-MM-DD') as date,
  sum(s.total) as total,count(*) as count from sale s
  where s.cancelled=false and s.sale_date>=:from and s.sale_date<:to group by 1 order by 1
  """,nativeQuery=true)
 List<Period> monthly(@Param("from") Instant from,@Param("to") Instant to);

 @Query(value="""
  select i.product_id as id,
  (array_agg(coalesce(i.product_name,p.name) order by s.sale_date desc,s.id desc,i.id desc))[1] as product,
  (array_agg(coalesce(i.product_code,p.code) order by s.sale_date desc,s.id desc,i.id desc))[1] as code,
  (array_agg(case when i.product_name is null then p.shade else i.shade end order by s.sale_date desc,s.id desc,i.id desc))[1] as shade,
  sum(i.quantity) as units from sale_item i join sale s on s.id=i.sale_id join product p on p.id=i.product_id
  where s.cancelled=false and s.sale_date>=:from and s.sale_date<:to
  group by i.product_id order by sum(i.quantity) desc,i.product_id asc limit 5
  """,nativeQuery=true)
 List<BestSeller> bestSellers(@Param("from") Instant from,@Param("to") Instant to);
}
