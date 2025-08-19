package com.feedfusion2.dto; // Adjust package if needed

import lombok.Getter;
import java.time.Instant;

@Getter
public class AccessToken {
    private final String token;
    private final Instant expiryTime;

    public AccessToken(String token, long expiresInSeconds) {
        this.token = token;
        // Calculate expiry time (add seconds, maybe subtract a small buffer like 60s)
        this.expiryTime = Instant.now().plusSeconds(expiresInSeconds - 60);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiryTime);
    }
}