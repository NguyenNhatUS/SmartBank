package com.SmartBank.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {
    @NotBlank(message = "Name can not be null")
    private String username;

    @NotBlank(message = "Email can not be null")
    private String email;

    @NotBlank(message = "Password can not be null")
    private String password;

}
