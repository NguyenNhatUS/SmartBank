package com.SmartBank.controller;

import com.SmartBank.security.RateLimit;

import com.SmartBank.dto.request.CreateEmployeeRequest;
import com.SmartBank.dto.request.LoginRequest;
import com.SmartBank.dto.request.RefreshRequest;
import com.SmartBank.dto.request.RegisterRequest;
import com.SmartBank.dto.response.LoginResponse;
import com.SmartBank.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/register")
    @RateLimit(requests = 3, duration = 60)
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", "Register successful"));
    }

    @PostMapping("/auth/login")
    @RateLimit(requests = 5, duration = 60)
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/auth/verify-otp")
    public ResponseEntity<LoginResponse> verifyOtp(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String code = request.get("code");
        return ResponseEntity.ok(authService.verifyOtp(username, code));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Map<String, String>> logout(
            Authentication authentication,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7);
        authService.logout(authentication.getName(), token);
        
        return ResponseEntity
                .ok(Map.of("message", "Logout successful"));
    }

    @PostMapping("/admin/employees")
    public ResponseEntity<Void> createEmployee(@RequestBody CreateEmployeeRequest request,
            Authentication authentication) {
        authService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
