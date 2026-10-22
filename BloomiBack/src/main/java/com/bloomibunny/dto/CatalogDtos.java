package com.bloomibunny.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public final class CatalogDtos {
 private CatalogDtos(){}
 public record NamedRequest(@NotBlank @Size(max=120) String name,@Size(max=500) String description,Boolean active){}
 public record NamedResponse(Long id,String name,String description,boolean active){}
 public record ContactRequest(@NotBlank @Size(max=140) String name,@Size(max=30) String phone,@Email @Size(max=150) String email,@Size(max=240) String address,@Size(max=1000) String notes,LocalDate birthday,Boolean active){}
 public record ContactResponse(Long id,String name,String phone,String email,String address,String notes,LocalDate birthday,boolean active){}
}
