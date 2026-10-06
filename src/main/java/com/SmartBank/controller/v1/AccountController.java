package com.SmartBank.controller.v1;

import com.SmartBank.dto.request.AccountCreateRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.dto.response.ApiResponse;
import com.SmartBank.dto.response.CustomerAccountResponse;
import com.SmartBank.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping()
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@RequestBody AccountCreateRequest request) {
        AccountResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Account created successfully", response));
    }

    @PreAuthorize("hasRole('ADMIN') or @accountSecurity.isOwner(#id, principal.username)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> getById(@PathVariable Long id, Principal principal) {
        AccountResponse response = service.getByID(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/freeze")
    public ResponseEntity<ApiResponse<AccountResponse>> freezeAccount(@PathVariable Long id) {
        AccountResponse response = service.freeze(id);
        return ResponseEntity.ok(ApiResponse.success("Account frozen successfully", response));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<ApiResponse<AccountResponse>> closeAccount(@PathVariable Long id) {
        AccountResponse response = service.close(id);
        return ResponseEntity.ok(ApiResponse.success("Account closed successfully", response));
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<CustomerAccountResponse>>> findAllGroupedByCustomer() {
        List<CustomerAccountResponse> response = service.findAllGroupedByCustomer();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/my")
    public ResponseEntity<ApiResponse<AccountResponse>> createMyAccount(
            @RequestBody AccountCreateRequest request,
            Authentication authentication) {
        AccountResponse response = service.createAccountForCustomer(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Customer account created successfully", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts(Authentication authentication) {
        List<AccountResponse> response = service.getAccountsByUsername(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}