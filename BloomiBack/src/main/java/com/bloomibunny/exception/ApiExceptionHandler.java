package com.bloomibunny.exception;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
@RestControllerAdvice
public class ApiExceptionHandler {
 record ApiError(String code,String message,Instant timestamp,String path,Map<String,String> fields){}
 @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class}) ResponseEntity<ApiError> malformed(Exception e,HttpServletRequest r){return response(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR",new RuntimeException("Revisa el formato de fechas, cantidades y opciones seleccionadas."),r,Map.of());}
 @ExceptionHandler(ResourceNotFoundException.class) ResponseEntity<ApiError> notFound(RuntimeException e,HttpServletRequest r){return response(HttpStatus.NOT_FOUND,"NOT_FOUND",e,r,Map.of());}
 @ExceptionHandler({BusinessException.class,IllegalArgumentException.class}) ResponseEntity<ApiError> business(RuntimeException e,HttpServletRequest r){return response(HttpStatus.BAD_REQUEST,"BUSINESS_ERROR",e,r,Map.of());}
 @ExceptionHandler(BadCredentialsException.class) ResponseEntity<ApiError> auth(RuntimeException e,HttpServletRequest r){return response(HttpStatus.UNAUTHORIZED,"INVALID_CREDENTIALS",new RuntimeException("Usuario o contraseña incorrectos"),r,Map.of());}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ApiError> validation(MethodArgumentNotValidException e,HttpServletRequest r){Map<String,String> f=new LinkedHashMap<>();e.getBindingResult().getFieldErrors().forEach(x->f.put(x.getField(),x.getDefaultMessage()));return response(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR",new RuntimeException("La solicitud contiene datos inválidos"),r,f);}
 @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class) ResponseEntity<ApiError> conflict(RuntimeException e,HttpServletRequest r){return response(HttpStatus.CONFLICT,"CONFLICT",new RuntimeException("El producto cambió mientras lo editabas. Cierra el formulario y vuelve a abrirlo para cargar los datos actuales."),r,Map.of());}
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) ResponseEntity<ApiError> duplicate(RuntimeException e,HttpServletRequest r){return response(HttpStatus.CONFLICT,"DATA_CONFLICT",new RuntimeException("El código ya existe o los datos entran en conflicto con otro registro."),r,Map.of());}
 private ResponseEntity<ApiError> response(HttpStatus s,String c,RuntimeException e,HttpServletRequest r,Map<String,String> f){return ResponseEntity.status(s).body(new ApiError(c,e.getMessage(),Instant.now(),r.getRequestURI(),f));}
}
