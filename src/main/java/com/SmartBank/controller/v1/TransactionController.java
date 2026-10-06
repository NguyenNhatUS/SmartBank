package com.SmartBank.controller.v1;

import com.SmartBank.dto.request.DepositWithDrawRequest;
import com.SmartBank.dto.request.TransferRequest;
import com.SmartBank.dto.response.ApiResponse;
import com.SmartBank.dto.response.PageResponse;
import com.SmartBank.dto.response.TransactionResponse;
import com.SmartBank.security.ratelimit.RateLimit;
import com.SmartBank.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @PostMapping("/deposit")
    @RateLimit(requests = 10, duration = 60)
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(@Valid @RequestBody DepositWithDrawRequest request) {
        TransactionResponse response = service.deposit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Deposit successful", response));
    }

    @PostMapping("/withdraw")
    @RateLimit(requests = 10, duration = 60)
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(@Valid @RequestBody DepositWithDrawRequest request) {
        TransactionResponse response = service.withdraw(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Withdraw successful", response));
    }

    @PostMapping("/transfer")
    @RateLimit(requests = 10, duration = 60)
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(@Valid @RequestBody TransferRequest request) {
        TransactionResponse response = service.transfer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Transfer successful", response));
    }

    @GetMapping("/account/{accountNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE') or @accountSecurity.isOwnerByAccountNumber(#accountNumber, authentication.name)")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getAccountStatement(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<TransactionResponse> statement = service.getAccountStatement(accountNumber, page, size);
        return ResponseEntity.ok(ApiResponse.success("Get account statement successfully", statement));
    }
}