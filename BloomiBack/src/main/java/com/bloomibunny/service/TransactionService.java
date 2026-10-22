package com.bloomibunny.service;
import com.bloomibunny.dto.TransactionDtos.*;
public interface TransactionService {SaleResponse sale(SaleRequest request);PurchaseResponse purchase(PurchaseRequest request);}
