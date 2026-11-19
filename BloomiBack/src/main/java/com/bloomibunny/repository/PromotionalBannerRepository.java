package com.bloomibunny.repository;
import com.bloomibunny.entity.PromotionalBanner;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface PromotionalBannerRepository extends JpaRepository<PromotionalBanner,Long> {
 @EntityGraph(attributePaths={"category","product"}) List<PromotionalBanner> findAllByOrderBySortOrderAscIdAsc();
 Optional<PromotionalBanner> findByRequestKey(UUID requestKey);
}
