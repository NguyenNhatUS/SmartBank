package com.SmartBank.security;

import com.SmartBank.entity.Customer;
import com.SmartBank.entity.Employee;
import com.SmartBank.entity.enums.CustomerStatus;
import com.SmartBank.exception.AppException;
import com.SmartBank.exception.ErrorCode;
import com.SmartBank.repository.CustomerRepository;
import com.SmartBank.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Rechecks current identity and state before accepting an access or refresh token. */
@Component
@RequiredArgsConstructor
public class UserAccessValidator {
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public void validate(String username, String role) {
        if (username == null || username.isBlank()
                || !("CUSTOMER".equals(role) || "EMPLOYEE".equals(role) || "ADMIN".equals(role))) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        var customer = customerRepository.findByUsername(username);
        var employee = employeeRepository.findByUsername(username);
        // Legacy duplicate usernames must never resolve to a different identity.
        if (customer.isPresent() && employee.isPresent()) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        if ("CUSTOMER".equals(role)) {
            Customer user = customer.orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));
            if (!user.isEnabled() || user.getStatus() != CustomerStatus.ACTIVE) {
                throw new AppException(ErrorCode.USER_NOT_ACTIVE);
            }
        } else {
            Employee user = employee.orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));
            if (user.getRole() == null || !role.equals(user.getRole().name())) {
                throw new AppException(ErrorCode.INVALID_TOKEN);
            }
            if (!user.isEnabled()) {
                throw new AppException(ErrorCode.USER_NOT_ACTIVE);
            }
        }
    }
}
