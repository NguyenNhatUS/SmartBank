package com.SmartBank.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "RefreshTokens", indexes = {
        @Index(name = "idx_refresh_token_username", columnList = "username"),
        @Index(name = "idx_refresh_token_expires_at", columnList = "expiresAt"),
        @Index(name = "idx_refresh_token_revoked_expires", columnList = "revoked, expiresAt")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private boolean revoked = false;
}
