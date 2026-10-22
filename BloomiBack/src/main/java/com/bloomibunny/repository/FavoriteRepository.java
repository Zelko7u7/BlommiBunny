package com.bloomibunny.repository;

import com.bloomibunny.entity.Favorite;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite,Long> {
 @Query("select f from Favorite f join fetch f.product p join fetch p.category left join fetch p.brand where f.user.username=:username and p.active=true and p.deleted=false order by f.id desc")
 List<Favorite> visible(@Param("username") String username);

 @Modifying
 @Query(value="insert into favorite (user_id,product_id,created_at,updated_at) values (:userId,:productId,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP) on conflict (user_id,product_id) do nothing",nativeQuery=true)
 void add(@Param("userId") Long userId,@Param("productId") Long productId);

 @Modifying
 @Query("delete from Favorite f where f.user.username=:username and f.product.id=:productId")
 void remove(@Param("username") String username,@Param("productId") Long productId);
}
