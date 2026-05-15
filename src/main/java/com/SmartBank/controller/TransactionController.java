package com.SmartBank.controller;

import com.SmartBank.security.RateLimit;
import com.SmartBank.dto.request.DepositWithDrawRequest;
import com.SmartBank.dto.request.TransferRequest;
import com.SmartBank.dto.request.TransactionFilterRequest;
import com.SmartBank.dto.request.BillPaymentRequest;
import com.SmartBank.dto.response.TransactionResponse;
import com.SmartBank.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @PostMapping("/deposit")
    @RateLimit(requests = 10, duration = 60)
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositWithDrawRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.deposit(request));
    }

    @PostMapping("/withdraw")
    @RateLimit(requests = 10, duration = 60)
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody DepositWithDrawRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.withdraw(request));
    }

    @PostMapping("/transfer")
    @RateLimit(requests = 10, duration = 60)
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.transfer(request));
    }

    @PostMapping("/pay-bill")
    @RateLimit(requests = 10, duration = 60)
    public ResponseEntity<TransactionResponse> payBill(@Valid @RequestBody BillPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.payBill(request));
    }

    @GetMapping("/filter")
    @RateLimit(requests = 20, duration = 60)
    public ResponseEntity<List<TransactionResponse>> filterTransactions(TransactionFilterRequest filter) {
        return ResponseEntity.ok(service.filterTransactions(filter));
    }
}