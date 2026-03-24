package com.SmartBank.service;

import com.SmartBank.dto.request.CustomerRequest;
import com.SmartBank.dto.response.CustomerResponse;
import com.SmartBank.mapper.CustomerMapper;
import com.SmartBank.model.Customer;
import com.SmartBank.model.enums.CustomerStatus;
import com.SmartBank.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Optional;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService service;

    private CustomerRequest request;
    private Customer customer;
    private CustomerResponse response;

    @BeforeEach
    void setUp() {
        request = CustomerRequest.builder()
                .fullName("Nguyen Van A")
                .email("a@gmail.com")
                .phone("0901234567")
                .address("HCM")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .build();

        customer = Customer.builder()
                .id(1)
                .fullName("Nguyen Van A")
                .email("a@gmail.com")
                .phone("0901234567")
                .status(CustomerStatus.ACTIVE)
                .build();

        response = CustomerResponse.builder()
                .id(1)
                .fullName("Nguyen Van A")
                .email("a@gmail.com")
                .build();
    }

    @Test
    public void geById_shouldReturnResponse_whenExists() {
        // Act
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        when(customerMapper.toResponse(customer)).thenReturn(response);

        CustomerResponse result = service.getById(1);

        assert(result.getFullName()).equals(response.getFullName());

        verify(customerRepository, times(1)).findById(1);

        verify(customerMapper, times(1)).toResponse(customer);

        // Assert
    }

}










