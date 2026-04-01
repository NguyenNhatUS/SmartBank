package com.SmartBank.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Name can not be null")
    private String username;
    @NotBlank(message = "Password can not be null")
    private String password;
}
