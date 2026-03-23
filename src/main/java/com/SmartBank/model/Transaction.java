package com.SmartBank.model;

import com.SmartBank.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "Transactions")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "transaction_code", nullable = false, unique = true, length = 25, updatable = false)
    private String transactionCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TransactionType type;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "source_account", nullable = false)
    private Account sourceAccount;

    @ManyToOne
    @JoinColumn(name = "target_account")
    private Account targetAccount;

    @PrePersist void onCreate() {
        if(createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
