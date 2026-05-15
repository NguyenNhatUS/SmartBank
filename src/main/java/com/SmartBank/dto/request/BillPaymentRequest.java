package com.SmartBank.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillPaymentRequest {
    @NotBlank(message = "Biller name is required")
    private String billerName;

    @NotBlank(message = "Bill number is required")
    private String billNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Account number is required")
    private String accountNumber;
}
