package com.SmartBank.controller;

import com.SmartBank.dto.request.CustomerRequest;
import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.dto.response.CustomerResponse;
import com.SmartBank.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping("/api/customers")
    public ResponseEntity<CustomerResponse> create(@RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping("/api/customers/{id}")
    public ResponseEntity<CustomerResponse> getCustomerByID(@PathVariable Integer id) {
        return ResponseEntity.ok()
                .body(service.getById(id));
    }

    @GetMapping("/api/all/customers")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        return ResponseEntity.ok()
                .body(service.getAllCustomers());
    }

    @PutMapping("/api/customers/{id}")
    public ResponseEntity<CustomerResponse> update(@PathVariable Integer id, @RequestBody CustomerRequest request) {
        CustomerResponse response = service.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/customers/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/customers/{id}/accounts")
    public ResponseEntity<List<AccountResponse>> getAccountsByID(@PathVariable Integer id) {
        return ResponseEntity.ok()
                .body(service.getAccountsByID(id));
    }


}