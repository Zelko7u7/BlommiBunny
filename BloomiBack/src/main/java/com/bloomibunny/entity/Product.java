package com.bloomibunny.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(uniqueConstraints=@UniqueConstraint(name="uk_product_code",columnNames="code"),indexes={@Index(name="idx_product_name",columnList="name"),@Index(name="idx_product_stock",columnList="stock")})
@Getter @Setter @NoArgsConstructor
public class Product extends BaseEntity {
 @Column(nullable=false,length=140) private String name;
 @Column(nullable=false,unique=true,length=60) private String code;
 @ManyToOne @JoinColumn(name="category_id",nullable=false) private Category category;
 @ManyToOne @JoinColumn(name="brand_id") private Brand brand;
 @ManyToOne @JoinColumn(name="supplier_id") private Supplier supplier;
 @Column(length=1200) private String description;
 @Column(length=80) private String shade;
 @Column(length=80) private String variantGroup;
 @Column(length=80) private String presentation;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal purchasePrice=BigDecimal.ZERO;
 @Column(nullable=false,precision=14,scale=2) private BigDecimal salePrice=BigDecimal.ZERO;
 @Column(nullable=false) private int stock;
 @Column(nullable=false) private int minimumStock;
 private LocalDate entryDate;
 @Column(nullable=false) private boolean active=true;
 @Column(length=500) private String imageUrl;
 @Column(nullable=false,columnDefinition="boolean default false") private boolean provisionalData=false;
 @Column(nullable=false,columnDefinition="boolean default true") private boolean costConfirmed=true;
 @Column(nullable=false,columnDefinition="boolean default false") private boolean deleted=false;
 @Version @Column(nullable=false,columnDefinition="bigint default 0") private long version;
}
