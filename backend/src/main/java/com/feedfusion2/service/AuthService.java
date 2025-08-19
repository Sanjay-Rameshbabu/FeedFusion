package com.feedfusion2.service;

import com.feedfusion2.dto.SignupRequest; // Ensure DTO exists
import com.feedfusion2.model.User; // Ensure User model exists
import com.feedfusion2.repository.UserRepository; // Ensure UserRepository exists
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // Ensure PasswordEncoder bean is configured
import org.springframework.stereotype.Service; // Make this a Spring Service bean

import java.util.HashSet;
import java.util.Set;

@Service // Marks this class as a Spring service component
public class AuthService {

    @Autowired // Inject the UserRepository bean
    UserRepository userRepository;

    @Autowired // Inject the PasswordEncoder bean (defined in SecurityConfig)
    PasswordEncoder passwordEncoder;

    /**
     * Registers a new user.
     * Checks for existing username/email, encodes the password, assigns default roles,
     * and saves the new user to the database.
     *
     * @param signupRequest DTO containing the new user's details.
     * @return true if registration is successful.
     * @throws IllegalArgumentException if username or email is already taken.
     */
    public boolean registerUser(SignupRequest signupRequest) {
        // 1. Check if username already exists
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            // Throw an exception that the controller can catch and return as a Bad Request
            throw new IllegalArgumentException("Error: Username is already taken!");
        }

        // 2. Check if email already exists
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            // Throw an exception
            throw new IllegalArgumentException("Error: Email is already in use!");
        }

        // 3. Create new user's account
        User user = new User(
                signupRequest.getUsername(),
                signupRequest.getEmail(),
                passwordEncoder.encode(signupRequest.getPassword()) // IMPORTANT: Encode the password
        );

        // 4. Set default roles (e.g., ROLE_USER) - adjust as needed
        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER"); // Assign a default role
        // You could add logic here to assign different roles based on signupRequest if needed
        user.setRoles(roles);

        // 5. Save the user to the database
        userRepository.save(user);

        // 6. Indicate successful registration
        return true;
    }
}
