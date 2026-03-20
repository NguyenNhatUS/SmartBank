package com.SmartBank.dto.request;


import com.SmartBank.model.Account;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {
    @NotNull(message = "source_account can non ben null")
    private Account sourceAccount;

    @NotNull(message = "source_account can non ben null")@NotNull(message = "source_account can non ben null")
    private Account targetAccount;

    @NotNull(message = "amount can not be null")
    @DecimalMin(value = "1000", message = "need at least 1000")
    private BigDecimal amount;

    @Size(max = 255)
    private String description;
}
