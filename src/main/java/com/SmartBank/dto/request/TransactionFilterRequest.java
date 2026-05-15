package com.SmartBank.dto.request;

import com.SmartBank.entity.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFilterRequest {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private TransactionType type;
    private String accountNumber;
}
