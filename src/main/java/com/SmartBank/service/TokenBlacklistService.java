package com.SmartBank.service;

import java.time.Instant;

public interface TokenBlacklistService {
    void blacklist(String token, Instant expiresAt);

    boolean isBlacklisted(String token);
}
