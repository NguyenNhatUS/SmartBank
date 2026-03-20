package com.SmartBank.dto.request;


import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositWithDrawRequest {
    @NotBlank(message = "account number can not be null")
    private String accountNumber;

    @NotNull(message = "Amount can not be null")
    @DecimalMin(value = "1000", message = "Min balance is 1,000 VNĐ")
    @DecimalMax(value = "500000000", message = "Max is 500,000,000 VNĐ each time")
    private BigDecimal amount;

    @Size(max = 255)
    private String description;
}
