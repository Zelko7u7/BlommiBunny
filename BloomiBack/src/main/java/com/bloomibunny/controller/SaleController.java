package com.bloomibunny.controller;

import com.bloomibunny.dto.SaleDtos.*;
import com.bloomibunny.entity.SaleChannel;
import com.bloomibunny.service.AdminSaleService;
import com.bloomibunny.service.SalePaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.LocalDate;

@RestController @RequestMapping("/api/sales")
public class SaleController {
 private final AdminSaleService service;
 private final SalePaymentService payments;
 public SaleController(AdminSaleService service,SalePaymentService payments){this.service=service;this.payments=payments;}
 @GetMapping public Page<SaleResponse> list(@RequestParam(defaultValue="") String q,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(required=false) Boolean cancelled,@RequestParam(required=false) SaleChannel channel,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="") String paymentStatus){return paymentStatus.isEmpty()?service.list(q,from,to,cancelled,channel,page,size):service.list(q,from,to,cancelled,channel,page,size,paymentStatus);}
 @GetMapping("/{id}") public SaleResponse get(@PathVariable Long id){return service.get(id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public SaleResponse create(@Valid @RequestBody CreateSaleRequest request,Principal actor){return service.create(request,actor.getName());}
 @PostMapping("/{id}/cancel") public SaleResponse cancel(@PathVariable Long id,@Valid @RequestBody CancelSaleRequest request,Principal actor){return service.cancel(id,request.reason(),actor.getName());}
 @PostMapping("/{id}/payments") @ResponseStatus(HttpStatus.CREATED)
 public SaleResponse payment(@PathVariable Long id,@Valid @RequestBody PaymentRequest request,Principal actor){return payments.add(id,request,actor.getName());}
 @PostMapping("/{id}/payments/{paymentId}/cancel")
 public SaleResponse cancelPayment(@PathVariable Long id,@PathVariable Long paymentId,@Valid @RequestBody CancelSaleRequest request,Principal actor){return payments.cancel(id,paymentId,request.reason(),actor.getName());}
 @PostMapping("/{id}/collection")
 public SaleResponse collection(@PathVariable Long id,@Valid @RequestBody CollectionRequest request,Principal actor){return payments.authorize(id,request,actor.getName());}
 @PostMapping("/{id}/payments/review")
 public SaleResponse review(@PathVariable Long id,@Valid @RequestBody ReviewPaymentsRequest request,Principal actor){return payments.review(id,request,actor.getName());}
}
