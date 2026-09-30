package com.SmartBank.service;

import com.SmartBank.dto.request.DepositWithDrawRequest;
import com.SmartBank.dto.request.TransferRequest;
import com.SmartBank.dto.response.TransactionResponse;
import com.SmartBank.mapper.TransactionMapper;
import com.SmartBank.entity.Account;
import com.SmartBank.entity.enums.AccountStatus;
import com.SmartBank.repository.AccountRepository;
import com.SmartBank.repository.TransactionRepository;
import com.SmartBank.exception.AppException;
import com.SmartBank.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.SmartBank.dto.response.PageResponse;
import com.SmartBank.entity.Transaction;
import org.springframework.data.domain.PageImpl;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import java.util.List;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @InjectMocks
    private TransactionServiceImpl service;

    @Mock
    private TransactionMapper mapper;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    private Account sourceAccount;
    private Account targetAccount;
    private DepositWithDrawRequest depositRequest;
    private TransferRequest transferRequest;


    @BeforeEach
    void setUp() {
        sourceAccount = Account.builder()
                .id(1L)
                .accountNumber("1234567890")
                .balance(new BigDecimal("1000000"))
                .status(AccountStatus.ACTIVE)
                .build();

        targetAccount = Account.builder()
                .id(2L)
                .accountNumber("0123456789")
                .balance(new BigDecimal("500000"))
                .status(AccountStatus.ACTIVE)
                .build();

        depositRequest = DepositWithDrawRequest.builder()
                .accountNumber("1234567890")
                .amount(new BigDecimal("200000"))
                .description("Nap tien")
                .build();

        transferRequest = TransferRequest.builder()
                .sourceAccountNumber("1234567890")
                .targetAccountNumber("0123456789")
                .amount(new BigDecimal("300000"))
                .build();
    }

    @Test
    void deposit_shouldIncreaseBalance_whenValidRequest() {
        when(accountRepository.findByAccountNumberWithLock(depositRequest.getAccountNumber())).thenReturn(sourceAccount);

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        when(mapper.toResponse((any()))).thenReturn(new TransactionResponse());

        service.deposit(depositRequest);

        assertEquals(new BigDecimal("1200000"), sourceAccount.getBalance());

        verify(accountRepository, times(1)).save(sourceAccount);
    }

    @Test
    public void withdraw_shouldThrow_whenInsufficientFunds() {
        when(accountRepository.findByAccountNumberWithLock(depositRequest.getAccountNumber())).thenReturn(sourceAccount);

        DepositWithDrawRequest withDrawRequest =
        DepositWithDrawRequest.builder()
                .accountNumber("1234567890")
                .amount(new BigDecimal("99999999999"))
                .description("Nap tien")
                .build();

        assertThatThrownBy(() -> service.withdraw(withDrawRequest)).isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid transaction amount");

        verify(accountRepository, never()).save(sourceAccount);

    }

    @Test
    public void withdraw_shouldDecreaseBalance_whenValidRequest() {
        when(accountRepository.findByAccountNumberWithLock(depositRequest.getAccountNumber())).thenReturn(sourceAccount);

        when(mapper.toResponse((any()))).thenAnswer(i -> i.getArgument(0));

        service.withdraw(depositRequest);

        assertEquals(new BigDecimal("800000"), sourceAccount.getBalance());

        verify(accountRepository, times(1)).save(sourceAccount);
    }

    @Test
    public void transfer_shouldSucceed_whenValidRequest() {
        when(accountRepository.findByAccountNumberWithLock(transferRequest.getSourceAccountNumber())).thenReturn(sourceAccount);

        when(accountRepository.findByAccountNumberWithLock(transferRequest.getTargetAccountNumber())).thenReturn(targetAccount);

        when(mapper.toResponse((any()))).thenAnswer(i -> i.getArgument(0));

        service.transfer(transferRequest);

        assertEquals(new BigDecimal("700000"), sourceAccount.getBalance());

        assertEquals(new BigDecimal("800000"), targetAccount.getBalance());


        verify(accountRepository, times(1)).save(sourceAccount);

        verify(accountRepository, times(1)).save(targetAccount);
    }

    @Test
    public void transfer_shouldThrow_whenInsufficientFunds() {
        when(accountRepository.findByAccountNumberWithLock(transferRequest.getSourceAccountNumber())).thenReturn(sourceAccount);

        when(accountRepository.findByAccountNumberWithLock(transferRequest.getTargetAccountNumber())).thenReturn(targetAccount);

        transferRequest.setAmount(new BigDecimal("999999999"));

        assertThatThrownBy(() -> service.transfer(transferRequest)).isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid transaction amount");

        verify(accountRepository, never()).save(sourceAccount);

        verify(accountRepository, never()).save(targetAccount);

    }

    @Test
    public void transfer_shouldThrow_whenAccountNotActive() {
        sourceAccount.setStatus(AccountStatus.FROZEN);
        targetAccount.setStatus(AccountStatus.FROZEN);

        when(accountRepository.findByAccountNumberWithLock(transferRequest.getSourceAccountNumber())).thenReturn(sourceAccount);

        when(accountRepository.findByAccountNumberWithLock(transferRequest.getTargetAccountNumber())).thenReturn(targetAccount);

        assertThatThrownBy(() -> service.transfer(transferRequest)).isInstanceOf(AppException.class)
                .hasMessageContaining("Account is inactive");

        verify(accountRepository, never()).save(sourceAccount);
    }

    @Test
    public void getAccountStatement_shouldReturnPageResponse_whenAccountExists() {
        String accountNumber = "ACC123";
        when(accountRepository.existsByAccountNumber(accountNumber)).thenReturn(true);

        Transaction txn = Transaction.builder()
                .id(1L)
                .transactionCode("TXN1001")
                .amount(new BigDecimal("500.00"))
                .sourceAccount(sourceAccount)
                .build();

        TransactionResponse txnResponse = TransactionResponse.builder()
                .id(1L)
                .transactionCode("TXN1001")
                .amount(new BigDecimal("500.00"))
                .sourceAccountNumber(accountNumber)
                .build();

        when(transactionRepository.findBySourceAccount_AccountNumberOrTargetAccount_AccountNumber(
                eq(accountNumber), eq(accountNumber), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(txn)));

        when(mapper.toResponse(txn)).thenReturn(txnResponse);

        PageResponse<TransactionResponse> result = service.getAccountStatement(accountNumber, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("TXN1001", result.getContent().get(0).getTransactionCode());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    public void getAccountStatement_shouldThrow_whenAccountNotFound() {
        String accountNumber = "NON_EXISTING";
        when(accountRepository.existsByAccountNumber(accountNumber)).thenReturn(false);

        assertThatThrownBy(() -> service.getAccountStatement(accountNumber, 0, 10))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Account not found");

        verify(transactionRepository, never()).findBySourceAccount_AccountNumberOrTargetAccount_AccountNumber(any(), any(), any());
    }
}