package com.SmartBank.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshRequest {
    @NotBlank(message = "Refresh token can not be blank")
    private String refreshToken;
}
