package com.SmartBank.service;

import com.SmartBank.dto.request.CustomerRequest;
import com.SmartBank.dto.response.CustomerResponse;
import com.SmartBank.exception.DuplicateResourceException;
import com.SmartBank.exception.ResourceNotFoundException;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public void create_shouldReturnResponse_whenValidRequest() {
        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);

        when(customerRepository.existsByPhone(request.getPhone())).thenReturn(false);

        when(customerMapper.toEntity(request)).thenReturn(customer);

        when(customerRepository.save(customer)).thenReturn(customer);

        when(customerMapper.toResponse(customer)).thenReturn(response);

        CustomerResponse result = service.create(request);

        assertEquals(response.getFullName(), result.getFullName());
        assertEquals(response.getId(), result.getId());

        verify(customerRepository, times(1)).save(any(Customer.class));

        verify(customerMapper, times(1)).toResponse(customer);
    }

    @Test
    public void geById_shouldReturnResponse_whenExists() {
        // Arrange
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        when(customerMapper.toResponse(customer)).thenReturn(response);

        // Act
        CustomerResponse result = service.getById(1);

        // Assert
        assert(result.getFullName()).equals(response.getFullName());

        verify(customerRepository, times(1)).findById(1);

        verify(customerMapper, times(1)).toResponse(customer);
    }

    @Test
    public void create_shouldThrow_whenEmailExists() {
        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email");

        verify(customerRepository, never()).save(customer);
    }

    @Test
    public void create_shouldThrow_whenPhoneExists() {
        when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);

        when(customerRepository.existsByPhone(request.getPhone())).thenReturn(true);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Phone");

        verify(customerRepository, never()).save(customer);
    }

    @Test
    public void getById_shouldThrow_whenCustomerNotFound() {
        when(customerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    public void deleteById_shouldThrow_whenCustomerNotFound() {
        when(customerRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    public void deleteById_shouldLockCustomer_whenExists() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        service.deleteById(1);

        assertEquals(customer.getStatus(), CustomerStatus.LOCKED);

        verify(customerRepository, times(1)).deleteById(1);

    }


}










