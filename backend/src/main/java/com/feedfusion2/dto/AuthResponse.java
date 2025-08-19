package com.feedfusion2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data // Lombok
@NoArgsConstructor // Lombok
@AllArgsConstructor // Lombok
public class AuthResponse {

    private String token;
    private String type = "Bearer"; // Standard token type
    private String id;
    private String username;
    private String email;
    private List<String> roles; // Send roles back if needed by frontend

    // Constructor used after successful login
    public AuthResponse(String token, String id, String username, String email, List<String> roles) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }
}