package com.SmartBank.dto.request;

import com.SmartBank.entity.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAccountCreateRequest {

    @NotNull(message = "Type can not be null")
    private AccountType type;
}
