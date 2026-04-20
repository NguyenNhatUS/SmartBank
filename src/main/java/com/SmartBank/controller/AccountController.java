package com.SmartBank.controller;

import com.SmartBank.dto.request.AccountCreateRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.dto.response.CustomerAccountResponse;
import com.SmartBank.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping()
    public ResponseEntity<AccountResponse> createAccount(@RequestBody AccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountByID(@PathVariable Long id) {
        return ResponseEntity.ok()
                .body(service.getByID(id));
    }

    @PatchMapping("/{id}/freeze")
    public ResponseEntity<AccountResponse> freezeAccount(@PathVariable Long id) {
        return ResponseEntity.ok()
                .body(service.freeze(id));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<AccountResponse> closeAccount(@PathVariable Long id) {
        return ResponseEntity.ok()
                .body(service.close(id));
    }

    @GetMapping()
    public ResponseEntity<List<CustomerAccountResponse>> findAllGroupedByCustomer() {
        return ResponseEntity.ok(service.findAllGroupedByCustomer());
    }

    @PostMapping("/my")
    public ResponseEntity<AccountResponse> createMyAccount(
            @RequestBody AccountCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createAccountForCustomer(authentication.getName(), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication authentication) {
        return ResponseEntity.ok(service.getAccountsByUsername(authentication.getName()));
    }
}