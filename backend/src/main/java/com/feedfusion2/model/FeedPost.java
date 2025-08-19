package com.feedfusion2.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data // Lombok: Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "feed_posts") // Maps this class to the MongoDB collection
public class FeedPost {

    @Id
    private String id; // MongoDB will generate this ObjectId, maps to _id

    private String title;
    private String description;
    private String author;

    @Indexed(unique = true) // Ensure links are unique
    private String link;

    private String mediaUrl; // Thumbnail URL

    @Indexed // Index for faster filtering
    private String platform; // "reddit" or "youtube"

    @Indexed // Index for sorting
    private Instant timestamp; // Use Instant for UTC timestamps

    @Indexed // Index for potential cleanup/TTL
    private Instant fetchedAt;

    private String videoId;
}