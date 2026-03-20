package com.SmartBank.dto.request;

import com.SmartBank.model.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCreateRequest {
    @NotNull(message = "CustomerID can not be null")
    private Integer customerId;

    @NotNull(message = "type can not be null")
    private AccountType type;
}
