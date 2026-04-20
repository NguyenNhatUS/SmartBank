package com.SmartBank.dto.request;

import com.SmartBank.entity.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCreateRequest {
    @NotNull(message = "CustomerID can not be null")
    private Long customerId;

    @NotNull(message = "Type can not be null")
    private AccountType type;
}
