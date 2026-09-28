package com.SmartBank.entity;

import com.SmartBank.entity.enums.AccountStatus;
import com.SmartBank.entity.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "Accounts", indexes = {
        @Index(name = "idx_account_status", columnList = "status"),
        @Index(name = "idx_account_customer_status", columnList = "customer_id, status"),
        @Index(name = "idx_account_type", columnList = "type"),
        @Index(name = "idx_account_created_at", columnList = "created_at")
})
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 25, updatable = false)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AccountType type;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private AccountStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = AccountStatus.ACTIVE;
        if (this.balance == null) this.balance = BigDecimal.ZERO;
    }
}
