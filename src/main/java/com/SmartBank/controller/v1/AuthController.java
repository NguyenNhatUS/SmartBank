package com.SmartBank.controller.v1;

import com.SmartBank.dto.request.CreateEmployeeRequest;
import com.SmartBank.dto.request.LoginRequest;
import com.SmartBank.dto.request.RefreshRequest;
import com.SmartBank.dto.request.RegisterRequest;
import com.SmartBank.dto.response.ApiResponse;
import com.SmartBank.dto.response.LoginResponse;
import com.SmartBank.security.ratelimit.RateLimit;
import com.SmartBank.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/register")
    @RateLimit(requests = 3, duration = 60)
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Register successful", null));
    }

    @PostMapping("/auth/login")
    @RateLimit(requests = 5, duration = 60)
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@RequestBody RefreshRequest request) {
        LoginResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return ResponseEntity
                .ok(ApiResponse.success("Logout successful", null));
    }

    @PostMapping("/admin/employees")
    public ResponseEntity<ApiResponse<Void>> createEmployee(@RequestBody CreateEmployeeRequest request) {
        authService.createEmployee(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Employee created successfully", null));
    }
}
