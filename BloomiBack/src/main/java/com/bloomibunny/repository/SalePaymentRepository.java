package com.bloomibunny.repository;
import com.bloomibunny.entity.SalePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SalePaymentRepository extends JpaRepository<SalePayment,Long> {
 Optional<SalePayment> findByRequestId(UUID requestId);
 List<SalePayment> findBySaleIdOrderByPaymentDateAscIdAsc(Long saleId);
}
