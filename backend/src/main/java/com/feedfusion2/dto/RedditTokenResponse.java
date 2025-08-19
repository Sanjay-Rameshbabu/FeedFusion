package com.feedfusion2.dto; // Adjust package if needed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore fields we don't need
public class RedditTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType; // Should be "bearer"

    @JsonProperty("expires_in")
    private long expiresIn; // Seconds until expiry

    @JsonProperty("scope")
    private String scope;

    // We don't usually need the refresh_token for script auth
}