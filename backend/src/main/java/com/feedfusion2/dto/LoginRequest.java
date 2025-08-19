
package com.feedfusion2.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data // Lombok
public class LoginRequest {

    @NotBlank(message = "Username cannot be blank")
    private String username; // Or email, depending on how you handle login

    @NotBlank(message = "Password cannot be blank")
    private String password;
}



