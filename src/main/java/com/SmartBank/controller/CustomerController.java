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
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping()
    public ResponseEntity<CustomerResponse> create(@RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerByID(@PathVariable Integer id) {
        return ResponseEntity.ok()
                .body(service.getById(id));
    }

    @GetMapping()
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        return ResponseEntity.ok()
                .body(service.getAllCustomers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> update(@PathVariable Integer id, @RequestBody CustomerRequest request) {
        CustomerResponse response = service.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/accounts")
    public ResponseEntity<List<AccountResponse>> getAccountsByID(@PathVariable Integer id) {
        return ResponseEntity.ok()
                .body(service.getAccountsByID(id));
    }


}