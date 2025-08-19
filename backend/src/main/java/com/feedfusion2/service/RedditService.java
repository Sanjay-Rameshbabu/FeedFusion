//package com.feedfusion2.service; // Ensure package/imports match your project
//
//import com.feedfusion2.model.FeedPost; // Ensure FeedPost is imported
//import com.feedfusion2.repository.FeedPostRepository;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.core.scheduler.Schedulers;
///
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Optional; // Ensure Optional is imported
//
//@Service
//public class RedditService {
//
//    private static final Logger log = LoggerFactory.getLogger(RedditService.class);
//    private final WebClient redditWebClient;
//    private final FeedPostRepository feedPostRepository;
//
//    @Autowired
//    public RedditService(@Qualifier("redditWebClient") WebClient redditWebClient,
//                         FeedPostRepository feedPostRepository) {
//        this.redditWebClient = Objects.requireNonNull(redditWebClient, "redditWebClient cannot be null");
//        this.feedPostRepository = Objects.requireNonNull(feedPostRepository, "feedPostRepository cannot be null");
//    }
//
//    public Flux<FeedPost> fetchRedditPosts(String interest) {
//        if (interest == null || interest.trim().isEmpty()) {
//            log.warn("fetchRedditPosts called with null or empty interest.");
//            return Flux.empty();
//        }
//        String trimmedInterest = interest.trim();
//        String subredditUri = String.format("/r/%s/hot.json?limit=20", trimmedInterest);
//        log.info("Fetching Reddit posts from URI: {}", subredditUri);
//
//        return redditWebClient.get()
//                .uri(subredditUri)
//                .retrieve()
//                .onStatus(status -> status.isError(), clientResponse ->
//                        clientResponse.bodyToMono(String.class)
//                                .defaultIfEmpty("[No Response Body]")
//                                .flatMap(body -> {
//                                    log.error("Reddit API call failed for interest '{}' with status {}: {}",
//                                            trimmedInterest, clientResponse.statusCode(), body);
//                                    return Mono.error(new RuntimeException("Reddit API request failed with status: " + clientResponse.statusCode()));
//                                })
//                )
//                .bodyToMono(Map.class)
//                .flatMapMany(this::parseRedditResponse) // This should return Flux<FeedPost>
//                // === Explicit typing within flatMap ===
//                .flatMap(feedPostObject -> { // Use a generic name first
//                    // Explicitly check and cast the object from the upstream Flux
//                    if (!(feedPostObject instanceof FeedPost)) {
//                        log.warn("Object in stream is not a FeedPost: {}", feedPostObject.getClass().getName());
//                        return Mono.empty(); // Skip if not the expected type
//                    }
//                    FeedPost feedPost = (FeedPost) feedPostObject; // Cast to FeedPost
//
//                    if (feedPost.getLink() == null) { // Now use the typed variable
//                        log.warn("Skipping post with null link during filtering stage.");
//                        return Mono.empty();
//                    }
//
//                    return Mono.fromCallable(() -> feedPostRepository.findByLink(feedPost.getLink())) // Use feedPost.getLink()
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .flatMap(optionalPostObject -> { // Use generic name first
//                                // Explicitly check and cast the object from the Mono result
//                                if (!(optionalPostObject instanceof Optional)) {
//                                    log.error("Repository call did not return Optional: {}", optionalPostObject.getClass().getName());
//                                    // Decide how to handle this - maybe error, maybe treat as empty
//                                    return Mono.empty(); // Or Mono.error(...)
//                                }
//                                @SuppressWarnings("unchecked") // Suppress warning as we checked type
//                                Optional<FeedPost> optionalPost = (Optional<FeedPost>) optionalPostObject; // Cast to Optional<FeedPost>
//
//                                if (optionalPost.isPresent()) { // Now use the typed variable
//                                    log.debug("Post already exists (blocking repo), filtering out: {}", feedPost.getLink());
//                                    return Mono.empty();
//                                } else {
//                                    log.debug("Post is new (blocking repo), keeping: {}", feedPost.getLink());
//                                    return Mono.just(feedPost); // Return the original FeedPost object
//                                }
//                            });
//                });
//                // === End of Blocking Call Handling ===
//                /*.doOnError(Throwable.class, throwableError -> {
//                    log.error("Error during Reddit processing chain for interest '{}': {}",
//                            trimmedInterest,
//                            throwableError.getMessage(), // getMessage here
//                            throwableError);
//                })
//                .onErrorResume(error -> Flux.empty());*/
//    }
//
//    // --- Parsing Logic (No changes needed from your previous version) ---
//    private Flux<FeedPost> parseRedditResponse(Map<String, Object> response) {
//        // ... Same robust parsing logic as before ...
//        // Ensure this method correctly returns Flux<FeedPost> or Flux.error()
//        List<FeedPost> posts = new ArrayList<>();
//        if (response == null) { /* ... */ return Flux.empty(); }
//        try {
//            // ... main parsing logic ...
//        } catch (ClassCastException | NullPointerException e) {
//            // *** Ensure this log call is correct ***
//            log.error("Error parsing Reddit response structure: {}", e.getMessage(), e);
//            return Flux.error(new RuntimeException("Failed to parse Reddit response structure", e));
//        } catch (Exception e) {
//            // *** Ensure this log call is correct ***
//            log.error("Unexpected error during Reddit response parsing: {}", e.getMessage(), e);
//            return Flux.error(new RuntimeException("Unexpected error parsing Reddit response", e));
//        }
//        return Flux.fromIterable(posts); // Important: Return the flux of posts
//    }
//}
//add contains working of youtube bur not reddit
package com.feedfusion2.service; // Ensure package/imports match your project

import com.feedfusion2.model.FeedPost; // Ensure FeedPost is imported
import com.feedfusion2.repository.FeedPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional; // Ensure Optional is imported

@Service
public class RedditService {

    private static final Logger log = LoggerFactory.getLogger(RedditService.class);
    private final WebClient redditWebClient;
    private final FeedPostRepository feedPostRepository;

    @Autowired
    public RedditService(@Qualifier("redditWebClient") WebClient redditWebClient,
                         FeedPostRepository feedPostRepository) {
        this.redditWebClient = Objects.requireNonNull(redditWebClient, "redditWebClient cannot be null");
        this.feedPostRepository = Objects.requireNonNull(feedPostRepository, "feedPostRepository cannot be null");
    }

    public Flux<FeedPost> fetchRedditPosts(String interest) {
        if (interest == null || interest.trim().isEmpty()) {
            log.warn("fetchRedditPosts called with null or empty interest.");
            return Flux.empty();
        }
        String trimmedInterest = interest.trim();
        String subredditUri = String.format("/r/%s/hot.json?limit=20", trimmedInterest);
        log.info("Fetching Reddit posts from URI: {}", subredditUri);

        return redditWebClient.get()
                .uri(subredditUri)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("[No Response Body]")
                                .flatMap(body -> {
                                    log.error("Reddit API call failed for interest '{}' with status {}: {}",
                                            trimmedInterest, clientResponse.statusCode(), body);
                                    return Mono.error(new RuntimeException("Reddit API request failed with status: " + clientResponse.statusCode()));
                                })
                )
                .bodyToMono(Map.class)
                .flatMapMany(this::parseRedditResponse) // This should return Flux<FeedPost>
                // === Explicit typing within flatMap ===
                .flatMap(feedPostObject -> { // Use a generic name first
                    // Explicitly check and cast the object from the upstream Flux
                    if (!(feedPostObject instanceof FeedPost)) {
                        log.warn("Object in stream is not a FeedPost: {}", feedPostObject.getClass().getName());
                        return Mono.empty(); // Skip if not the expected type
                    }
                    FeedPost feedPost = (FeedPost) feedPostObject; // Cast to FeedPost

                    if (feedPost.getLink() == null) { // Now use the typed variable
                        log.warn("Skipping post with null link during filtering stage.");
                        return Mono.empty();
                    }

                    return Mono.fromCallable(() -> feedPostRepository.findByLink(feedPost.getLink())) // Use feedPost.getLink()
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(optionalPostObject -> { // Use generic name first
                                // Explicitly check and cast the object from the Mono result
                                if (!(optionalPostObject instanceof Optional)) {
                                    log.error("Repository call did not return Optional: {}", optionalPostObject.getClass().getName());
                                    // Decide how to handle this - maybe error, maybe treat as empty
                                    return Mono.empty(); // Or Mono.error(...)
                                }
                                @SuppressWarnings("unchecked") // Suppress warning as we checked type
                                Optional<FeedPost> optionalPost = (Optional<FeedPost>) optionalPostObject; // Cast to Optional<FeedPost>

                                if (optionalPost.isPresent()) { // Now use the typed variable
                                    log.debug("Post already exists (blocking repo), filtering out: {}", feedPost.getLink());
                                    return Mono.empty();
                                } else {
                                    log.debug("Post is new (blocking repo), keeping: {}", feedPost.getLink());
                                    return Mono.just(feedPost); // Return the original FeedPost object
                                }
                            });
                })
                // === End of Blocking Call Handling ===
                // === RESTORED Error Handling ===
                .doOnError(Throwable.class, error -> { // Use generic 'error' parameter name
                    // Explicitly check and cast 'error' before using methods
                    if (error instanceof Throwable) { // Check if it's actually a Throwable
                        Throwable throwableError = (Throwable) error; // Cast it
                        log.error("Error during Reddit processing chain for interest '{}': {}",
                                trimmedInterest,
                                throwableError.getMessage(), // Use casted variable's method
                                throwableError); // Log the casted variable
                    } else {
                        // Log if something unexpected (non-Throwable) was caught
                        log.error("Non-Throwable error intercepted in doOnError for interest '{}': {}", trimmedInterest, error);
                    }
                })
                .onErrorResume(error -> Flux.empty()); // Simple resume
    }

    // --- Parsing Logic (Assuming details exist in the try block) ---
  /*  private Flux<FeedPost> parseRedditResponse(Map<String, Object> response) {
        List<FeedPost> posts = new ArrayList<>();
        if (response == null) {
            log.warn("parseRedditResponse called with null response map.");
            return Flux.empty();
        }
        try {
            // --- Assume your detailed parsing logic is here ---
            // Example structure (needs actual implementation based on previous discussions):
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) { log.warn("Reddit response missing 'data'"); return Flux.empty(); }
            List<Map<String, Object>> children = (List<Map<String, Object>>) data.get("children");
            if (children == null) { log.warn("Reddit response missing 'children'"); return Flux.empty(); }

            for (Map<String, Object> child : children) {
                Map<String, Object> postData = (Map<String, Object>) child.get("data");
                if (postData == null) { continue; }
                // ... (null checks for title, permalink etc.) ...
                FeedPost post = new FeedPost();
                // ... (post.setTitle(...), post.setLink(...), etc.) ... // <<<<< Ensure this logic is actually present in your real code
                posts.add(post);
            }
            // --- End of assumed detailed parsing logic ---

        } catch (ClassCastException | NullPointerException e) {
            // *** Corrected log call format ***
            log.error("Error parsing Reddit response structure: {}", e.getMessage(), e);
            return Flux.error(new RuntimeException("Failed to parse Reddit response structure", e));
        } catch (Exception e) {
            // *** Corrected log call format ***
            log.error("Unexpected error during Reddit response parsing: {}", e.getMessage(), e);
            return Flux.error(new RuntimeException("Unexpected error parsing Reddit response", e));
        }
        log.debug("Successfully parsed {} Reddit posts.", posts.size()); // Log count before returning
        return Flux.fromIterable(posts);
    }*/
    // --- COMPLETE Parsing Logic for Reddit API Response ---
    private Flux<FeedPost> parseRedditResponse(Map<String, Object> response) {
        List<FeedPost> posts = new ArrayList<>();
        if (response == null) {
            log.warn("parseRedditResponse called with null response map.");
            return Flux.empty();
        }
        try {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
                log.warn("Reddit response missing 'data' field.");
                return Flux.empty();
            }
            List<Map<String, Object>> children = (List<Map<String, Object>>) data.get("children");
            if (children == null) {
                log.warn("Reddit response missing 'data.children' field.");
                return Flux.empty();
            }

            log.debug("Processing {} items from Reddit response.", children.size());

            for (Map<String, Object> child : children) {
                // Check if 'child' map contains 'data' key and it's a Map
                if (!(child.get("data") instanceof Map)) {
                    log.warn("Skipping child with missing or invalid 'data' field: {}", child);
                    continue;
                }
                Map<String, Object> postData = (Map<String, Object>) child.get("data");

                // Basic null checks for essential fields
                if (postData.get("title") == null || postData.get("permalink") == null || postData.get("author") == null) {
                    log.warn("Skipping post with missing title, permalink, or author: {}", postData.get("id")); // Log post ID if available
                    continue;
                }

                FeedPost post = new FeedPost(); // Create the post object

                // --- Populate the post object ---
                post.setPlatform("reddit");
                post.setTitle(String.valueOf(postData.get("title")));
                post.setAuthor(String.valueOf(postData.get("author")));
                // Construct the full link
                String permalink = String.valueOf(postData.get("permalink"));
                post.setLink("https://www.reddit.com" + permalink);

                // Description (selftext can be empty)
                post.setDescription(String.valueOf(postData.getOrDefault("selftext", "")));
                if (post.getDescription().length() > 500) { // Truncate
                    post.setDescription(post.getDescription().substring(0, 497) + "...");
                }

                // Thumbnail (check if it's a valid URL string)
                Object thumbnailObj = postData.get("thumbnail");
                if (thumbnailObj instanceof String && ((String) thumbnailObj).startsWith("http")) {
                    post.setMediaUrl((String) thumbnailObj);
                } else {
                    post.setMediaUrl(null); // Set to null if invalid or missing
                }

                // Timestamp (created_utc is seconds since epoch)
                Object createdUtcObj = postData.get("created_utc");
                if (createdUtcObj instanceof Number) {
                    try {
                        // Convert Double/Long seconds to Instant
                        long epochSeconds = ((Number) createdUtcObj).longValue();
                        post.setTimestamp(Instant.ofEpochSecond(epochSeconds));
                    } catch (Exception e) {
                        log.warn("Could not parse created_utc value '{}' for post {}: {}", createdUtcObj, post.getLink(), e.getMessage());
                        post.setTimestamp(Instant.now()); // Fallback
                    }
                } else {
                    log.warn("Missing or invalid 'created_utc' field for post: {}", post.getLink());
                    post.setTimestamp(Instant.now()); // Fallback
                }

                post.setFetchedAt(Instant.now());
                // --- Finished populating ---

                log.debug("Successfully parsed Reddit post: {}", post.getLink());
                posts.add(post); // Add the populated post
            }
        } catch (ClassCastException | NullPointerException e) {
            log.error("Error parsing Reddit response structure: {}", e.getMessage(), e);
            return Flux.error(new RuntimeException("Failed to parse Reddit response structure", e));
        } catch (Exception e) {
            log.error("Unexpected error during Reddit response parsing: {}", e.getMessage(), e);
            return Flux.error(new RuntimeException("Unexpected error parsing Reddit response", e));
        }
        log.info("Finished parsing Reddit response. {} posts extracted.", posts.size());
        return Flux.fromIterable(posts);
    }

}