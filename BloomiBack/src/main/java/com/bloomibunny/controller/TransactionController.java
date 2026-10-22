package com.bloomibunny.controller;
import com.bloomibunny.dto.TransactionDtos.*;
import com.bloomibunny.service.AdminPurchaseService;
import com.bloomibunny.service.AdminExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.security.Principal;
@RestController @RequestMapping("/api")
public class TransactionController {
 private final AdminPurchaseService purchases;private final AdminExpenseService expenses;
 public TransactionController(AdminPurchaseService purchases,AdminExpenseService expenses){this.purchases=purchases;this.expenses=expenses;}
 @PostMapping("/purchases") @ResponseStatus(HttpStatus.CREATED) PurchaseResponse purchase(@Valid @RequestBody PurchaseRequest r,Principal actor){return purchases.create(r,actor.getName());}
 @GetMapping("/purchases") History<PurchaseResponse> purchases(@RequestParam(defaultValue="") String q,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(required=false) Boolean cancelled,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return purchases.list(q,from,to,cancelled,page,size);}
 @GetMapping("/purchases/{id}") PurchaseResponse purchase(@PathVariable Long id){return purchases.get(id);}
 @PostMapping("/purchases/{id}/cancel") PurchaseResponse cancelPurchase(@PathVariable Long id,@Valid @RequestBody CancelRequest r,Principal actor){return purchases.cancel(id,r.reason(),actor.getName());}
 @PostMapping("/expenses") @ResponseStatus(HttpStatus.CREATED) ExpenseResponse expense(@Valid @RequestBody ExpenseRequest r,Principal actor){return expenses.create(r,actor.getName());}
 @GetMapping("/expenses") History<ExpenseResponse> expenses(@RequestParam(defaultValue="") String q,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(required=false) Boolean cancelled,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return expenses.list(q,from,to,cancelled,page,size);}
 @GetMapping("/expenses/{id}") ExpenseResponse expense(@PathVariable Long id){return expenses.get(id);}
 @PostMapping("/expenses/{id}/cancel") ExpenseResponse cancelExpense(@PathVariable Long id,@Valid @RequestBody CancelRequest r,Principal actor){return expenses.cancel(id,r.reason(),actor.getName());}
}
