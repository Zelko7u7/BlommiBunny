package com.bloomibunny.dto;
import com.bloomibunny.entity.PromotionalBanner.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
public final class BannerDtos {
 private BannerDtos(){}
 public record Image(@NotBlank @Size(max=500) String url,@NotBlank @Size(max=200) String publicId,@Min(1) @Max(10000) int width,@Min(1) @Max(10000) int height){}
 public record Request(@NotNull UUID requestKey,@NotBlank @Size(max=120) String internalName,@NotNull @Valid Image desktopImage,@Valid Image mobileImage,@NotBlank @Size(max=240) String altText,@Size(max=120) String headline,@Size(max=240) String description,@NotNull TargetType targetType,Long targetReferenceId,PublicSection section,@Min(0) @Max(10000) int sortOrder,boolean active,Instant startsAt,Instant endsAt,Long expectedVersion){}
 public record Status(boolean active,@NotNull Long expectedVersion){}
 public record Position(@NotNull Long id,@NotNull Long expectedVersion){}
 public record Reorder(@NotNull @Size(max=50) List<@NotNull @Valid Position> banners){}
 public record Option(Long id,String name){}
 public record SectionOption(PublicSection id,String name){}
 public record Targets(List<Option> categories,List<Option> products,List<SectionOption> sections){}
 public record Response(Long id,String internalName,Image desktopImage,Image mobileImage,String altText,String headline,String description,TargetType targetType,Long targetReferenceId,PublicSection section,int sortOrder,boolean active,Instant startsAt,Instant endsAt,long version,UUID requestKey,String targetLabel,boolean targetAvailable,String publicationStatus,String href){}
 public record PublicBanner(Long id,Image desktopImage,Image mobileImage,String altText,String headline,String description,String href,Instant endsAt){}
}
