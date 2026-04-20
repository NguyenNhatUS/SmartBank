package com.SmartBank.dto.request;

import com.SmartBank.entity.enums.Role;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateEmployeeRequest {
    private String username;
    private String password;
    private Role role; // only EMPLOYEE or ADMIN
}
