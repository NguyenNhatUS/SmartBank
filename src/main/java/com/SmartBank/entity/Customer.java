package com.SmartBank.entity;

import com.SmartBank.entity.enums.CustomerStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "Customers")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "full_name", length = 1000)
    private String fullName;

    @Column(unique = true, length = 100)
    private String email;

    @Column(unique = true, length = 12)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CustomerStatus status;

    @Column(name = "created_at",  updatable = false)
    private LocalDateTime createdAt;

    @Column(unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private boolean enabled = true;

    @OneToMany(
            mappedBy = "customer",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    private List<Account> accountList = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = CustomerStatus.ACTIVE;
    }

}
