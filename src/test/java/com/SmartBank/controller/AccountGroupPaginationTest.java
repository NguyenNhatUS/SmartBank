package com.SmartBank.controller;

import com.SmartBank.controller.v1.AccountController;
import com.SmartBank.entity.Customer;
import com.SmartBank.exception.GlobalExceptionHandler;
import com.SmartBank.mapper.AccountMapper;
import com.SmartBank.repository.AccountRepository;
import com.SmartBank.repository.CustomerRepository;
import com.SmartBank.service.impl.AccountServiceImpl;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real service/mapper/advice, mocked repositories; this does not exercise security filters.
@ExtendWith(MockitoExtension.class)
class AccountGroupPaginationTest {
    @Mock private CustomerRepository customers;
    @Mock private AccountRepository accounts;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var service = new AccountServiceImpl(accounts, Mappers.getMapper(AccountMapper.class), customers);
        mvc = MockMvcBuilders.standaloneSetup(new AccountController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void defaultPageIncludesCustomersWithoutAccounts() throws Exception {
        PageRequest request = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
        Customer customer = Customer.builder().id(7L).fullName("Customer Seven").email("seven@example.com").build();
        when(customers.findAll(request)).thenReturn(new PageImpl<>(List.of(customer), request, 1));
        when(accounts.findAllByCustomerIdsWithCustomer(List.of(7L))).thenReturn(List.of());

        mvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].customerId").value(7))
                .andExpect(jsonPath("$.data.content[0].customerName").value("Customer Seven"))
                .andExpect(jsonPath("$.data.content[0].customerEmail").value("seven@example.com"))
                .andExpect(jsonPath("$.data.content[0].accounts").isEmpty())
                .andExpect(jsonPath("$.data.pageNo").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void pageBeyondEndKeepsCustomerTotalsAndSkipsAccountQuery() throws Exception {
        PageRequest request = PageRequest.of(5, 2, Sort.by(Sort.Direction.DESC, "id"));
        when(customers.findAll(request)).thenReturn(new PageImpl<>(List.of(), request, 3));
        mvc.perform(get("/api/v1/accounts").param("page", "5").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.pageNo").value(5))
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.last").value(true));
        verifyNoInteractions(accounts);
    }

    @Test
    void maximumPageSizeIsAccepted() throws Exception {
        PageRequest request = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "id"));
        when(customers.findAll(request)).thenReturn(Page.empty(request));
        mvc.perform(get("/api/v1/accounts").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(100));
        verifyNoInteractions(accounts);
    }

    @ParameterizedTest
    @CsvSource({"-1,10", "0,0", "0,-1", "0,101", "abc,10", "0,abc", "2147483648,10"})
    void invalidParametersReturn400BeforeAnyQuery(String page, String size) throws Exception {
        mvc.perform(get("/api/v1/accounts").param("page", page).param("size", size))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.path").value("/api/v1/accounts"));
        verifyNoInteractions(customers, accounts);
    }
}
