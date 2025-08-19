package com.feedfusion2.model;

import lombok.Data; // Optional Lombok annotation
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Data // Lombok: Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: Generates no-args constructor
@Document(collection = "users") // Specifies the MongoDB collection name
public class User {

    @Id
    private String id; // MongoDB typically uses String IDs

    @Indexed(unique = true) // Ensure username is unique
    private String username;

    @Indexed(unique = true) // Ensure email is unique
    private String email;

    private String password; // Store hashed password

    // Roles for authorization (optional, but good practice)
    private Set<String> roles = new HashSet<>();
    // Stores the MongoDB IDs (_id) of the bookmarked FeedPost documents
    private Set<String> bookmarkedPostIds = new HashSet<>();
    // Constructor without ID (useful for creation)
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.roles.add("ROLE_USER"); // Add default role
    }
    public void addBookmark(String postId) {
        if (this.bookmarkedPostIds == null) {
            this.bookmarkedPostIds = new HashSet<>();
        }
        this.bookmarkedPostIds.add(postId);
    }

    public void removeBookmark(String postId) {
        if (this.bookmarkedPostIds != null) {
            this.bookmarkedPostIds.remove(postId);
        }
    }

    // If not using Lombok, generate getters, setters, constructors, etc. manually
}
