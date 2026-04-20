package com.SmartBank.dto.response;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Integer id;
    private String transactionCode;
    private String type;
    private BigDecimal amount;
    private String description;
    private LocalDateTime createdAt;

    private String sourceAccountNumber;
    private String targetAccountNumber;  // null if DEPOSIT/WITHDRAWAL
}
