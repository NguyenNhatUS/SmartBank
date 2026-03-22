package com.SmartBank.controller;

import com.SmartBank.dto.request.AccountCreateRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.dto.response.CustomerAccountResponse;
import com.SmartBank.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping("/api/accounts")
    public ResponseEntity<AccountResponse> createAccount(@RequestBody AccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping("/api/accounts/{id}")
    public ResponseEntity<AccountResponse> getAccountByID(@PathVariable Integer id) {
        return ResponseEntity.ok()
                .body(service.getByID(id));
    }

    @PatchMapping("/api/accounts/{id}/freeze")
    public ResponseEntity<AccountResponse> freezeAccount(@PathVariable Integer id) {
        return ResponseEntity.ok()
                .body(service.freeze(id));
    }

    @PatchMapping("/api/accounts/{id}/close")
    public ResponseEntity<AccountResponse> closeAccount(@PathVariable Integer id) {
        return ResponseEntity.ok()
                .body(service.close(id));
    }

    @GetMapping("/api/accounts")
    public ResponseEntity<List<CustomerAccountResponse>> findAllGroupedByCustomer() {
        return ResponseEntity.ok(service.findAllGroupedByCustomer());
    }

}