package com.SmartBank.mapper;

import com.SmartBank.dto.request.CustomerRequest;
import com.SmartBank.dto.response.CustomerResponse;
import com.SmartBank.model.Customer;

public class CustomerMapper {
    public Customer toEntity(CustomerRequest request) {
        return Customer.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .dateOfBirth(request.getDateOfBirth())
                .build();
    }

    public CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .dateOfBirth(customer.getDateOfBirth())
                .build();
    }

}
