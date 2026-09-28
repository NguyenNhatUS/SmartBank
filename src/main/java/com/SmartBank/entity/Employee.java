package com.SmartBank.entity;

import com.SmartBank.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "Employees", indexes = {
        @Index(name = "idx_employee_role_enabled", columnList = "role, enabled")
})
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // EMPLOYEE, ADMIN

    private boolean enabled = true;
}