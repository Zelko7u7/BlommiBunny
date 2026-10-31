package com.bloomibunny.service;

import com.bloomibunny.dto.SaleDtos.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.exception.*;
import com.bloomibunny.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.ZoneId;

@Service @Transactional
public class SalePaymentService {
 private final SaleRepository sales;
 private final SalePaymentRepository payments;
 private final SaleCollectionEventRepository events;
 private final OperationSupport support;
 private final AdminSaleService details;
 public SalePaymentService(SaleRepository sales,SalePaymentRepository payments,SaleCollectionEventRepository events,OperationSupport support,AdminSaleService details){
  this.sales=sales;this.payments=payments;this.events=events;this.support=support;this.details=details;
 }
 public SaleResponse add(Long saleId,PaymentRequest request,String actor){
  var fingerprint=support.request(request.requestId(),saleId+":"+request);
  var sale=lock(saleId);
  var previous=payments.findByRequestId(request.requestId());
  if(previous.isPresent()){
   if(!previous.get().getRequestFingerprint().equals(fingerprint))throw new BusinessException("Esta solicitud ya registró otro abono. Consulta el historial.");
   return details.get(saleId);
  }
  active(sale);
  if(!sale.isPaymentsReviewed())throw new BusinessException("Revisa primero los pagos anteriores de esta venta.");
  var amount=support.amount(request.amount());
  if(amount.signum()<=0)throw new BusinessException("El abono debe ser mayor que cero.");
  if(request.paymentMethod()==null)throw new BusinessException("Selecciona el medio de pago.");
  support.date(request.date());
  if(request.date().isBefore(sale.getSaleDate().atZone(ZoneId.of("America/Bogota")).toLocalDate()))throw new BusinessException("La fecha del abono no puede ser anterior a la venta.");
  if(paid(saleId).add(amount).compareTo(sale.getTotal())>0)throw new BusinessException("El abono supera el saldo pendiente. Actualiza el detalle.");
  var payment=new SalePayment();payment.setSale(sale);payment.setRequestId(request.requestId());payment.setRequestFingerprint(fingerprint);
  payment.setAmount(amount);payment.setPaymentDate(request.date());payment.setPaymentMethod(request.paymentMethod());
  payment.setReference(support.clean(request.reference()));payment.setNotes(support.clean(request.notes()));payment.setRecordedBy(actor);
  payments.saveAndFlush(payment);sale.setPaymentsChangedAt(support.now());sales.flush();
  return details.get(saleId);
 }
 public SaleResponse cancel(Long saleId,Long paymentId,String reason,String actor){
  var sale=lock(saleId);
  var payment=payments.findById(paymentId).filter(p->p.getSale().getId().equals(saleId)).orElseThrow(()->new ResourceNotFoundException("Abono no encontrado en esta venta."));
  if(payment.isCancelled())return details.get(saleId);
  active(sale);var checked=support.reason(reason);
  payment.setCancelled(true);payment.setCancelledAt(support.now());payment.setCancellationReason(checked);payment.setCancelledBy(actor);
  revoke(sale,"Corrección de abono #"+paymentId,actor);
  sale.setPaymentsChangedAt(support.now());payments.flush();sales.flush();return details.get(saleId);
 }
 public SaleResponse authorize(Long saleId,CollectionRequest request,String actor){
  var sale=lock(saleId);active(sale);version(sale,request.expectedVersion());
  var reason=support.reason(request.reason());
  if(request.authorized()){
   if(!sale.isPaymentsReviewed()||paid(saleId).compareTo(sale.getTotal())!=0)throw new BusinessException("Solo puedes autorizar el cobro cuando los pagos estén revisados y el saldo sea cero.");
   if(!sale.isCollectionAuthorized()){
    sale.setCollectionAuthorized(true);sale.setCollectionAuthorizedAt(support.now());sale.setCollectionAuthorizedBy(actor);event(sale,"AUTHORIZED",reason,actor);
   }
  }else revoke(sale,reason,actor);
  sales.flush();return details.get(saleId);
 }
 public SaleResponse review(Long saleId,ReviewPaymentsRequest request,String actor){
  var sale=lock(saleId);active(sale);version(sale,request.expectedVersion());
  if(sale.isPaymentsReviewed())throw new BusinessException("Los pagos de esta venta ya fueron revisados.");
  sale.setPaymentsReviewed(true);sale.setPaymentsReviewedAt(support.now());sale.setPaymentsReviewedBy(actor);
  event(sale,"REVIEWED",support.reason(request.reason()),actor);sales.flush();return details.get(saleId);
 }
 private Sale lock(Long id){return sales.lockById(id).orElseThrow(()->new ResourceNotFoundException("Venta no encontrada."));}
 private void active(Sale sale){if(sale.isCancelled())throw new BusinessException("La venta está anulada. Sus abonos se conservan como historial; la devolución del dinero se gestiona por separado.");}
 private void version(Sale sale,Long expected){if(expected==null||sale.getVersion()!=expected)throw new BusinessException("La venta cambió. Actualiza el detalle antes de continuar.");}
 private BigDecimal paid(Long id){return payments.findBySaleIdOrderByPaymentDateAscIdAsc(id).stream().filter(p->!p.isCancelled()).map(SalePayment::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);}
 private void revoke(Sale sale,String reason,String actor){if(sale.isCollectionAuthorized()){sale.setCollectionAuthorized(false);sale.setCollectionAuthorizedAt(null);sale.setCollectionAuthorizedBy(null);event(sale,"REVOKED",reason,actor);}}
 private void event(Sale sale,String action,String reason,String actor){var event=new SaleCollectionEvent();event.setSale(sale);event.setAction(action);event.setReason(reason);event.setRecordedBy(actor);events.save(event);}
}
