package com.bloomibunny.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="promotional_banner") @Getter @Setter @NoArgsConstructor
public class PromotionalBanner extends BaseEntity {
 public enum TargetType {CATALOG,CATEGORY,PRODUCT,SECTION}
 public enum PublicSection {ABOUT,FAVORITES}
 @Column(nullable=false,length=120) private String internalName;
 @Column(nullable=false,length=500) private String desktopImageUrl;
 @Column(nullable=false,length=200) private String desktopImagePublicId;
 @Column(nullable=false) private int desktopWidth;
 @Column(nullable=false) private int desktopHeight;
 @Column(length=500) private String mobileImageUrl;
 @Column(length=200) private String mobileImagePublicId;
 private Integer mobileWidth;
 private Integer mobileHeight;
 @Column(nullable=false,length=240) private String altText;
 @Column(length=120) private String headline;
 @Column(length=240) private String description;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TargetType targetType;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="category_id") private Category category;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id") private Product product;
 @Enumerated(EnumType.STRING) @Column(length=20) private PublicSection section;
 @Column(nullable=false) private int sortOrder;
 @Column(nullable=false) private boolean active;
 private Instant startsAt;
 private Instant endsAt;
 @Column(nullable=false,unique=true) private UUID requestKey;
 @Column(unique=true,length=40) private String legacyKey;
 @Version @Column(nullable=false) private long version;
}
