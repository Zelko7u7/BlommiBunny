package com.bloomibunny.controller;

import com.bloomibunny.dto.SaleDtos.*;
import com.bloomibunny.entity.SaleChannel;
import com.bloomibunny.service.AdminSaleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.LocalDate;

@RestController @RequestMapping("/api/sales")
public class SaleController {
 private final AdminSaleService service;
 public SaleController(AdminSaleService service){this.service=service;}
 @GetMapping public Page<SaleResponse> list(@RequestParam(defaultValue="") String q,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(required=false) Boolean cancelled,@RequestParam(required=false) SaleChannel channel,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.list(q,from,to,cancelled,channel,page,size);}
 @GetMapping("/{id}") public SaleResponse get(@PathVariable Long id){return service.get(id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public SaleResponse create(@Valid @RequestBody CreateSaleRequest request,Principal actor){return service.create(request,actor.getName());}
 @PostMapping("/{id}/cancel") public SaleResponse cancel(@PathVariable Long id,@Valid @RequestBody CancelSaleRequest request,Principal actor){return service.cancel(id,request.reason(),actor.getName());}
}
