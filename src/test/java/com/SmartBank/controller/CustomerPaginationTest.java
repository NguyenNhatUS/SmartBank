package com.SmartBank.controller;

import com.SmartBank.controller.v1.CustomerController;
import com.SmartBank.entity.Customer;
import com.SmartBank.entity.enums.CustomerStatus;
import com.SmartBank.exception.GlobalExceptionHandler;
import com.SmartBank.mapper.AccountMapper;
import com.SmartBank.mapper.CustomerMapper;
import com.SmartBank.repository.CustomerRepository;
import com.SmartBank.repository.EmployeeRepository;
import com.SmartBank.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Tests the HTTP contract with real service, mapper and advice; no security filter chain.
@ExtendWith(MockitoExtension.class)
class CustomerPaginationTest {
    @Mock private CustomerRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        CustomerServiceImpl service = new CustomerServiceImpl(repository, mock(EmployeeRepository.class),
                Mappers.getMapper(CustomerMapper.class), mock(AccountMapper.class), mock(PasswordEncoder.class));
        mvc = MockMvcBuilders.standaloneSetup(new CustomerController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void defaultsReturnPageEnvelopeInsteadOfAnArray() throws Exception {
        PageRequest request = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
        Customer customer = Customer.builder().id(7L).fullName("Customer Seven")
                .status(CustomerStatus.ACTIVE).build();
        when(repository.findAll(request)).thenReturn(new PageImpl<>(List.of(customer), request, 1));
        when(repository.countAccountsByCustomerIds(List.of(7L))).thenReturn(List.of());

        mvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(7))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Customer Seven"))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.content[0].totalAccounts").value(0))
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.data.pageNo").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void explicitPageAndSizePreserveMetadataForOutOfRangePage() throws Exception {
        PageRequest request = PageRequest.of(5, 2, Sort.by(Sort.Direction.DESC, "id"));
        when(repository.findAll(request)).thenReturn(new PageImpl<>(List.of(), request, 3));

        mvc.perform(get("/api/v1/customers").param("page", "5").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.pageNo").value(5))
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.last").value(true));
        verify(repository, never()).countAccountsByCustomerIds(anyList());
    }

    @Test
    void acceptsMaximumPageSize() throws Exception {
        PageRequest request = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "id"));
        when(repository.findAll(request)).thenReturn(Page.empty(request));
        mvc.perform(get("/api/v1/customers").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(100));
    }

    @ParameterizedTest
    @CsvSource({"-1,10", "0,0", "0,-1", "0,101", "abc,10", "0,abc", "2147483648,10"})
    void rejectsInvalidPaginationWith400WithoutQueryingDatabase(String page, String size) throws Exception {
        mvc.perform(get("/api/v1/customers").param("page", page).param("size", size))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.path").value("/api/v1/customers"));
        verifyNoInteractions(repository);
    }
}
