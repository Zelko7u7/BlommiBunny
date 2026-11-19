package com.bloomibunny.controller;
import com.bloomibunny.dto.CatalogDtos.NamedResponse;
import com.bloomibunny.repository.CategoryRepository;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public class StoreCategoryController {
 private final CategoryRepository categories;public StoreCategoryController(CategoryRepository c){categories=c;}
 @GetMapping("/api/store/categories") public List<NamedResponse> list(){return categories.findAll().stream().filter(c->c.isActive()).map(c->new NamedResponse(c.getId(),c.getName(),c.getDescription(),true)).sorted(Comparator.comparing(NamedResponse::name)).toList();}
}
