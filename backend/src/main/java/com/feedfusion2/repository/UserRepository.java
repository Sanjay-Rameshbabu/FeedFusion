package com.feedfusion2.repository;

import com.feedfusion2.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> { // Use String for ID type

    // Method to find a user by their username
    Optional<User> findByUsername(String username);

    // Method to find a user by their email
    Optional<User> findByEmail(String email);

    // Method to check if a username already exists
    Boolean existsByUsername(String username);

    // Method to check if an email already exists
    Boolean existsByEmail(String email);
}
