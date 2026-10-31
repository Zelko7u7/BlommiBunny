package com.bloomibunny.repository;
import com.bloomibunny.entity.SaleCollectionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SaleCollectionEventRepository extends JpaRepository<SaleCollectionEvent,Long> {
 List<SaleCollectionEvent> findBySaleIdOrderByIdAsc(Long saleId);
}
