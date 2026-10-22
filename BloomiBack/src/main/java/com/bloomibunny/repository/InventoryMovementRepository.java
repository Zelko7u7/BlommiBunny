package com.bloomibunny.repository;
import com.bloomibunny.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement,Long>{List<InventoryMovement> findTop10ByOrderByCreatedAtDesc();}
