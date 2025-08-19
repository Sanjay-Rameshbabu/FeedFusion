package com.feedfusion2.service;

import com.feedfusion2.model.User;
import com.feedfusion2.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
// Ensure this import exists and is correct:
import org.springframework.stereotype.Service;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional; // Optional but good

import java.util.List;
import java.util.stream.Collectors;

// --- V V V MAKE SURE THIS ANNOTATION IS PRESENT V V V ---
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional // Good practice for database operations
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Find user by username (or email, if you allow login with email)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        // Convert user roles (Set<String>) to Spring Security authorities (List<GrantedAuthority>)
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role)) // Assuming roles are stored like "ROLE_USER", "ROLE_ADMIN"
                .collect(Collectors.toList());

        // Return Spring Security User object
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(), // Password should be hashed in the database
                authorities);
    }
}
