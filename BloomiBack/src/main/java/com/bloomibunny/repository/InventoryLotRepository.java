package com.bloomibunny.repository;

import com.bloomibunny.entity.InventoryLot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryLotRepository extends JpaRepository<InventoryLot,Long> {
 List<InventoryLot> findByProductIdOrderByOpeningDescReceivedOnAscReceivedAtAscSourceKeyAsc(Long productId);
 List<InventoryLot> findByProductIdIn(java.util.Collection<Long> productIds);
}
