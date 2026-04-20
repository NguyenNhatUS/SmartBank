package com.SmartBank.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAccountResponse {
    private Long customerId;

    private String customerName;

    private String customerEmail;

    private List<AccountResponse> accounts;
}
