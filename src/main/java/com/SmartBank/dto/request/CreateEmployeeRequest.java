package com.SmartBank.dto.request;

import com.SmartBank.entity.enums.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateEmployeeRequest {
    private String username;
    private String password;
    private Role role; // only EMPLOYEE or ADMIN
}
