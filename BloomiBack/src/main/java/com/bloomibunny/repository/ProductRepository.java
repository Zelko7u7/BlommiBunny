package com.bloomibunny.repository;
import com.bloomibunny.entity.Product;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface ProductRepository extends JpaRepository<Product,Long>{
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @Query("select p from Product p where p.id=:id")
 java.util.Optional<Product> lockById(@Param("id") Long id);
 List<Product> findByVariantGroupAndActiveTrueAndDeletedFalseOrderByShadeAsc(String variantGroup);
 @Query("select p from Product p where p.deleted=false and (lower(p.name) like lower(concat('%',:q,'%')) or lower(p.code) like lower(concat('%',:q,'%')))")
 Page<Product> search(@Param("q") String q,Pageable pageable);
 List<Product> findByStockLessThanEqualOrderByStockAsc(int stock);
 long countByStock(int stock);
 Page<Product> findByDeletedFalse(Pageable pageable);
 boolean existsByCode(String code);
 boolean existsByCodeAndIdNot(String code,Long id);
 long countByDeletedFalse();
 long countByStockAndDeletedFalse(int stock);
}
