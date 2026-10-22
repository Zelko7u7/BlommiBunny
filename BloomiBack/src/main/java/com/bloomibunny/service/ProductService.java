package com.bloomibunny.service;
import com.bloomibunny.dto.ProductDtos.*;
import org.springframework.data.domain.Page;
public interface ProductService {java.util.List<ProductResponse> variants(String group);com.bloomibunny.dto.InventorySummary summary(String q);Page<ProductResponse> list(String q,int page,int size,String sort);ProductResponse get(Long id);ProductResponse create(ProductRequest r);ProductResponse update(Long id,ProductRequest r);ProductResponse adjust(Long id,StockAdjustment r);ProductResponse setActive(Long id,boolean active);void delete(Long id);}
