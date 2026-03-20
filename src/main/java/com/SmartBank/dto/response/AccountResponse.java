package com.SmartBank.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private String type;
    private BigDecimal balance;
    private String status;
    private LocalDateTime createdAt;

    private Integer customerId;
    private String customerName;
}