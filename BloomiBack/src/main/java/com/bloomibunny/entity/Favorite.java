package com.bloomibunny.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="favorite", uniqueConstraints=@UniqueConstraint(name="uk_favorite_user_product", columnNames={"user_id","product_id"}))
@Getter @Setter @NoArgsConstructor
public class Favorite extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id",nullable=false) private AppUser user;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="product_id",nullable=false) private Product product;
}
