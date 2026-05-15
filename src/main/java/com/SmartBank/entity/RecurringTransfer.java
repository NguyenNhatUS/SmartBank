package com.SmartBank.entity;

import com.SmartBank.entity.enums.Frequency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "recurring_transfers")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecurringTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_account", nullable = false)
    private Account sourceAccount;

    @ManyToOne
    @JoinColumn(name = "target_account", nullable = false)
    private Account targetAccount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    @Column(name = "next_execution_date", nullable = false)
    private LocalDateTime nextExecutionDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    private String description;
}
