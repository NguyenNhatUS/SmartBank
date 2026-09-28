package com.SmartBank.mapper;

import com.SmartBank.dto.request.CustomerRequest;
import com.SmartBank.dto.response.CustomerResponse;
import com.SmartBank.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "accountList", ignore = true)
    Customer toEntity(CustomerRequest request);

    @Mapping(target = "totalAccounts", expression = "java(customer.getAccountList() == null ? 0 : customer.getAccountList().size())")
    CustomerResponse toResponse(Customer customer);
}
