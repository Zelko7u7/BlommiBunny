package com.bloomibunny.service;

import com.bloomibunny.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Component
public class OperationSupport {
 private final JdbcTemplate jdbc;private final Clock clock;
 public OperationSupport(JdbcTemplate jdbc,Clock clock){this.jdbc=jdbc;this.clock=clock;}
 public String request(UUID key,Object payload){
  if(key==null)throw new BusinessException("La solicitud debe tener un identificador.");
  jdbc.queryForObject("select 1 from pg_advisory_xact_lock(?)",Integer.class,key.getMostSignificantBits()^key.getLeastSignificantBits());
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.toString().getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 public BigDecimal amount(BigDecimal value){if(value==null||value.signum()<0||value.stripTrailingZeros().scale()>2||value.compareTo(new BigDecimal("999999999999.99"))>0)throw new BusinessException("Revisa los importes: hasta dos decimales, sin valores negativos y dentro del límite permitido.");return value;}
 public void date(LocalDate date){if(date==null||date.isAfter(LocalDate.now(clock.withZone(ZoneId.of("America/Bogota")))))throw new BusinessException("Indica una fecha válida, que no sea futura.");}
 public void filters(LocalDate from,LocalDate to,int page,int size){if(page<0||size<1||size>100)throw new BusinessException("Paginación inválida.");if(from!=null&&to!=null&&from.isAfter(to))throw new BusinessException("La fecha inicial no puede superar la final.");}
 public String reason(String value){var reason=clean(value);if(reason==null||reason.length()<5||reason.length()>500)throw new BusinessException("Explica el motivo de anulación (entre 5 y 500 caracteres).");return reason;}
 public String clean(String value){return value==null||value.isBlank()?null:value.trim();}
 public Instant now(){return clock.instant();}
}
