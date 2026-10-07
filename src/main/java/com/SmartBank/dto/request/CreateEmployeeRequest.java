package com.SmartBank.dto.request;

import com.SmartBank.entity.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateEmployeeRequest {
    @NotBlank(message = "Username can not be blank")
    private String username;

    @NotBlank(message = "Password can not be blank")
    private String password;

    @NotNull(message = "Role can not be null")
    private Role role; // only EMPLOYEE or ADMIN
}
