package com.bloomibunny.controller;
import com.bloomibunny.dto.TransactionDtos.*;
import com.bloomibunny.entity.Expense;
import com.bloomibunny.repository.*;
import com.bloomibunny.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/api")
public class TransactionController {
 private final TransactionService service;private final SaleRepository sales;private final PurchaseRepository purchases;private final ExpenseRepository expenses;
 public TransactionController(TransactionService s,SaleRepository a,PurchaseRepository p,ExpenseRepository e){service=s;sales=a;purchases=p;expenses=e;}
 @PostMapping("/sales") @ResponseStatus(HttpStatus.CREATED) SaleResponse sale(@Valid @RequestBody SaleRequest r){return service.sale(r);}
 @GetMapping("/sales") List<?> sales(){return sales.findAll();}
 @PostMapping("/purchases") @ResponseStatus(HttpStatus.CREATED) PurchaseResponse purchase(@Valid @RequestBody PurchaseRequest r){return service.purchase(r);}
 @GetMapping("/purchases") List<?> purchases(){return purchases.findAll();}
 @GetMapping("/expenses") List<Expense> expenses(){return expenses.findAll();}
 @PostMapping("/expenses") @ResponseStatus(HttpStatus.CREATED) Expense expense(@Valid @RequestBody ExpenseRequest r){var e=new Expense();e.setCategory(r.category());e.setDescription(r.description());e.setAmount(r.amount());e.setExpenseDate(r.expenseDate()==null?LocalDate.now():r.expenseDate());return expenses.save(e);}
 @DeleteMapping("/expenses/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteExpense(@PathVariable Long id){expenses.deleteById(id);}
}
