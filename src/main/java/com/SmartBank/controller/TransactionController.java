package com.SmartBank.controller;

import com.SmartBank.dto.request.DepositWithDrawRequest;
import com.SmartBank.dto.request.TransferRequest;
import com.SmartBank.dto.response.TransactionResponse;
import com.SmartBank.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping("/api/transactions/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositWithDrawRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.deposit(request));
    }

    @PostMapping("/api/transactions/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody DepositWithDrawRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.withdraw(request));
    }

    @PostMapping("/api/transactions/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.transfer(request));
    }


}