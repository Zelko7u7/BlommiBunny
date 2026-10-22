package com.bloomibunny.service;

import com.bloomibunny.dto.TransactionDtos.*;
import com.bloomibunny.entity.Expense;
import com.bloomibunny.exception.*;
import com.bloomibunny.repository.ExpenseRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDate;

@Service @Transactional
public class AdminExpenseService {
 private final ExpenseRepository expenses;private final OperationSupport ops;
 public AdminExpenseService(ExpenseRepository expenses,OperationSupport ops){this.expenses=expenses;this.ops=ops;}
 public ExpenseResponse create(ExpenseRequest request,String actor){
  var fingerprint=ops.request(request.requestId(),request);var previous=expenses.findByRequestId(request.requestId());
  if(previous.isPresent()){
   if(!fingerprint.equals(previous.get().getRequestFingerprint()))throw new BusinessException("La solicitud ya registró otro gasto. Consulta el historial.");
   return map(previous.get());
  }
  ops.date(request.date());var amount=ops.amount(request.amount());
  if(amount.signum()==0||ops.clean(request.category())==null||ops.clean(request.description())==null||request.paymentMethod()==null)throw new BusinessException("Completa categoría, concepto, importe positivo y medio de pago.");
  var expense=new Expense();expense.setCategory(request.category().trim());expense.setDescription(request.description().trim());expense.setAmount(amount);expense.setExpenseDate(request.date());expense.setPaymentMethod(request.paymentMethod());expense.setReference(ops.clean(request.reference()));expense.setNotes(ops.clean(request.notes()));expense.setRecordedBy(actor);expense.setRequestId(request.requestId());expense.setRequestFingerprint(fingerprint);
  return map(expenses.saveAndFlush(expense));
 }
 public ExpenseResponse cancel(Long id,String reason,String actor){
  var expense=expenses.lockById(id).orElseThrow(()->new ResourceNotFoundException("Gasto no encontrado"));if(expense.isCancelled())return map(expense);
  expense.setCancellationReason(ops.reason(reason));expense.setCancelled(true);expense.setCancelledAt(ops.now());expense.setCancelledBy(actor);expenses.flush();return map(expense);
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public History<ExpenseResponse> list(String q,LocalDate from,LocalDate to,Boolean cancelled,int page,int size){
  ops.filters(from,to,page,size);var search=q==null?"":q.trim();
  var rows=expenses.search(search,from,to,cancelled,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"expenseDate","id")));var totals=expenses.totals(search,from,to,cancelled);
  return new History<>(rows.map(this::map).getContent(),rows.getTotalElements(),rows.getTotalPages(),rows.getNumber(),totals.getConfirmedTotal(),totals.getConfirmedCount(),totals.getCancelledCount());
 }
 @Transactional(readOnly=true) public ExpenseResponse get(Long id){return map(expenses.findById(id).orElseThrow(()->new ResourceNotFoundException("Gasto no encontrado")));}
 private ExpenseResponse map(Expense e){return new ExpenseResponse(e.getId(),e.getCategory(),e.getDescription(),e.getAmount(),e.getExpenseDate(),e.getPaymentMethod(),e.getReference(),e.getNotes(),e.getRecordedBy(),e.isCancelled(),e.getCancelledAt(),e.getCancellationReason(),e.getCancelledBy());}
}
