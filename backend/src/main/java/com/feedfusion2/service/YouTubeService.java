//package com.feedfusion2.service; // Ensure package/imports match your project
//
//import com.feedfusion2.model.FeedPost;
//import com.feedfusion2.repository.FeedPostRepository;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.core.scheduler.Schedulers; // Import Schedulers
//
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Optional; // Import Optional
//
//
//@Service
//public class YouTubeService {
//
//    private static final Logger log = LoggerFactory.getLogger(YouTubeService.class);
//    private final WebClient youtubeWebClient;
//    private final FeedPostRepository feedPostRepository; // Assuming blocking repo
//
//    @Value("${youtube.api.key}")
//    private String apiKey;
//
//    @Autowired
//    public YouTubeService(@Qualifier("youtubeWebClient") WebClient youtubeWebClient,
//                          FeedPostRepository feedPostRepository) { // Assuming blocking repo injection
//        this.youtubeWebClient = Objects.requireNonNull(youtubeWebClient);
//        this.feedPostRepository = Objects.requireNonNull(feedPostRepository);
//    }
//
//    // fetchYouTubeVideos method remains the same as your last working version
//    // including the restored error handling .doOnError/.onErrorResume
//    public Flux<FeedPost> fetchYouTubeVideos(String interest) {
//        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_YOUTUBE_API_KEY_HERE")) {
//            log.warn("YouTube API Key is not configured. Skipping YouTube fetch for interest '{}'.", interest);
//            return Flux.empty();
//        }
//        if (interest == null || interest.trim().isEmpty()) {
//            log.warn("fetchYouTubeVideos called with null or empty interest.");
//            return Flux.empty();
//        }
//        String trimmedInterest = interest.trim();
//
//        // *** UPDATED maxResults to 3 ***
//        String searchUrl = String.format("/search?part=snippet&q=%s&type=video&key=%s&maxResults=3", // Changed to 3
//                trimmedInterest.replace(" ", "+"), apiKey);
//        log.info("Fetching YouTube videos for query: {}", trimmedInterest);
//
//        return youtubeWebClient.get()
//                .uri(searchUrl)
//                .retrieve()
//                .onStatus(status -> status.isError(), clientResponse ->
//                        clientResponse.bodyToMono(String.class)
//                                .defaultIfEmpty("[No Response Body]")
//                                .flatMap(body -> {
//                                    log.error("YouTube API call failed for interest '{}' with status {}: {}",
//                                            trimmedInterest, clientResponse.statusCode(), body);
//                                    return Mono.error(new RuntimeException("YouTube API request failed with status: " + clientResponse.statusCode()));
//                                })
//                )
//                .bodyToMono(Map.class) // Or specific YouTube DTO
//                .flatMapMany(this::parseYouTubeResponse) // This should return Flux<FeedPost>
//                // === Explicit typing within flatMap ===
//                .flatMap(feedPostObject -> { // Use a generic name first
//                    // Explicitly check and cast the object from the upstream Flux
//                    if (!(feedPostObject instanceof FeedPost)) {
//                        log.warn("YouTube: Object in stream is not a FeedPost: {}", feedPostObject.getClass().getName());
//                        return Mono.empty();
//                    }
//                    FeedPost feedPost = (FeedPost) feedPostObject; // Cast to FeedPost
//
//                    // Check link *before* DB call (it should be set by parseYouTubeResponse)
//                    if (feedPost.getLink() == null) {
//                        log.warn("YouTube: Skipping post with null link before DB check.");
//                        return Mono.empty();
//                    }
//
//                    // Check DB for duplicates
//                    return Mono.fromCallable(() -> feedPostRepository.findByLink(feedPost.getLink())) // Use feedPost.getLink()
//                            .subscribeOn(Schedulers.boundedElastic())
//                            .flatMap(optionalPostObject -> { // Use generic name first
//                                // Explicitly check and cast the object from the Mono result
//                                if (!(optionalPostObject instanceof Optional)) {
//                                    log.error("YouTube: Repository call did not return Optional: {}", optionalPostObject.getClass().getName());
//                                    return Mono.empty();
//                                }
//                                @SuppressWarnings("unchecked")
//                                Optional<FeedPost> optionalPost = (Optional<FeedPost>) optionalPostObject; // Cast to Optional<FeedPost>
//
//                                if (optionalPost.isPresent()) { // Now use the typed variable
//                                    log.debug("YouTube: Post already exists (blocking repo), filtering out: {}", feedPost.getLink());
//                                    return Mono.empty();
//                                } else {
//                                    log.debug("YouTube: Post is new (blocking repo), keeping: {}", feedPost.getLink());
//                                    return Mono.just(feedPost); // Return the original FeedPost object
//                                }
//                            });
//                }) // <<< End of flatMap for repo check
//                // === RESTORED Error Handling ===
//                .doOnError(Throwable.class, error -> { // Use generic 'error' parameter name
//                    // Explicitly check and cast 'error' before using methods
//                    if (error instanceof Throwable) { // Check if it's actually a Throwable
//                        Throwable throwableError = (Throwable) error; // Cast it
//                        log.error("Error during YouTube processing chain for interest '{}': {}",
//                                trimmedInterest,
//                                throwableError.getMessage(), // Use casted variable's method
//                                throwableError); // Log the casted variable
//                    } else {
//                        log.error("Non-Throwable error intercepted in YouTube doOnError for interest '{}': {}", trimmedInterest, error);
//                    }
//                })
//                .onErrorResume(error -> Flux.empty()); // Simple resume
//    }
//
//
//    // --- COMPLETE Parsing Logic for YouTube API Response (with videoId added) ---
//    private Flux<FeedPost> parseYouTubeResponse(Map<String, Object> response) {
//        List<FeedPost> posts = new ArrayList<>();
//        if (response == null) {
//            log.warn("parseYouTubeResponse called with null response map.");
//            return Flux.empty();
//        }
//
//        try {
//            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
//            if (items == null) {
//                log.warn("YouTube response missing 'items' array.");
//                return Flux.empty();
//            }
//
//            log.debug("Processing {} items from YouTube response.", items.size());
//
//            for (Map<String, Object> item : items) {
//                Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
//                Map<String, Object> idMap = (Map<String, Object>) item.get("id"); // Renamed to avoid conflict with FeedPost id field
//
//                // --- Essential Checks ---
//                if (idMap == null || !"youtube#video".equals(idMap.get("kind")) || idMap.get("videoId") == null || snippet == null) {
//                    log.warn("Skipping non-video item or item with missing ID/snippet: {}", item);
//                    continue;
//                }
//
//                String videoId = String.valueOf(idMap.get("videoId"));
//                if (videoId.isEmpty()) { // Extra check
//                    log.warn("Skipping item with empty videoId: {}", item);
//                    continue;
//                }
//                // Construct the link (still useful for direct linking if needed)
//                String videoLink = "https://www.youtube.com/watch?v=dQw4w9WgXcQ" + videoId;
//
//                FeedPost post = new FeedPost();
//                post.setPlatform("youtube");
//                post.setLink(videoLink);
//                // *** SET videoId ***
//                post.setVideoId(videoId); // <<< This line was added/confirmed
//
//                // --- Populate other fields from snippet ---
//                post.setTitle(String.valueOf(snippet.getOrDefault("title", "Untitled")));
//                post.setDescription(String.valueOf(snippet.getOrDefault("description", "")));
//                if (post.getDescription().length() > 500) {
//                    post.setDescription(post.getDescription().substring(0, 497) + "...");
//                }
//                post.setAuthor(String.valueOf(snippet.getOrDefault("channelTitle", "Unknown Channel")));
//
//                // Thumbnails
//                Map<String, Object> thumbnails = (Map<String, Object>) snippet.get("thumbnails");
//                if (thumbnails != null) {
//                    Map<String, Object> thumb = (Map<String, Object>) thumbnails.get("medium");
//                    if (thumb == null) {
//                        thumb = (Map<String, Object>) thumbnails.get("default");
//                    }
//                    if (thumb != null && thumb.get("url") instanceof String) {
//                        post.setMediaUrl((String) thumb.get("url"));
//                    }
//                }
//
//                // Timestamp
//                Object publishedAtObj = snippet.get("publishedAt");
//                if (publishedAtObj instanceof String) {
//                    try {
//                        post.setTimestamp(Instant.parse((String) publishedAtObj));
//                    } catch (Exception pe) {
//                        log.warn("Could not parse YouTube timestamp '{}' for videoId {}: {}", publishedAtObj, videoId, pe.getMessage());
//                        post.setTimestamp(Instant.now()); // Fallback
//                    }
//                } else {
//                    log.warn("Missing or invalid 'publishedAt' field for videoId {}", videoId);
//                    post.setTimestamp(Instant.now()); // Fallback
//                }
//
//                post.setFetchedAt(Instant.now());
//
//                log.debug("Successfully parsed YouTube video: {}", post.getLink());
//                posts.add(post); // Add the fully populated post
//            }
//        } catch (ClassCastException | NullPointerException e) {
//            log.error("Error parsing YouTube response structure: {}", e.getMessage(), e);
//            return Flux.error(new RuntimeException("Failed to parse YouTube response structure", e));
//        } catch (Exception e) {
//            log.error("Unexpected error parsing YouTube response: {}", e.getMessage(), e);
//            return Flux.error(new RuntimeException("Unexpected error parsing YouTube response", e));
//        }
//
//        log.info("Finished parsing YouTube response. {} posts extracted.", posts.size());
//        return Flux.fromIterable(posts);
//    }
//}
package com.feedfusion2.service; // Ensure package/imports match your project

import com.feedfusion2.model.FeedPost;
import com.feedfusion2.repository.FeedPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers; // Import Schedulers

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional; // Import Optional


@Service
public class YouTubeService {

    private static final Logger log = LoggerFactory.getLogger(YouTubeService.class);
    private final WebClient youtubeWebClient;
    private final FeedPostRepository feedPostRepository; // Assuming blocking repo

    @Value("${youtube.api.key}")
    private String apiKey;

    @Autowired
    public YouTubeService(@Qualifier("youtubeWebClient") WebClient youtubeWebClient,
                          FeedPostRepository feedPostRepository) { // Assuming blocking repo injection
        this.youtubeWebClient = Objects.requireNonNull(youtubeWebClient);
        this.feedPostRepository = Objects.requireNonNull(feedPostRepository);
    }

    // fetchYouTubeVideos method remains the same as your last working version
    // including the restored error handling .doOnError/.onErrorResume
    public Flux<FeedPost> fetchYouTubeVideos(String interest) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_YOUTUBE_API_KEY_HERE")) {
            log.warn("YouTube API Key is not configured. Skipping YouTube fetch for interest '{}'.", interest);
            return Flux.empty();
        }
        if (interest == null || interest.trim().isEmpty()) {
            log.warn("fetchYouTubeVideos called with null or empty interest.");
            return Flux.empty();
        }
        String trimmedInterest = interest.trim();

        // *** UPDATED maxResults to 3 ***
        String searchUrl = String.format("/search?part=snippet&q=%s&type=video&key=%s&maxResults=3", // Changed to 3
                trimmedInterest.replace(" ", "+"), apiKey);
        log.info("Fetching YouTube videos for query: {}", trimmedInterest);

        return youtubeWebClient.get()
                .uri(searchUrl)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("[No Response Body]")
                                .flatMap(body -> {
                                    log.error("YouTube API call failed for interest '{}' with status {}: {}",
                                            trimmedInterest, clientResponse.statusCode(), body);
                                    return Mono.error(new RuntimeException("YouTube API request failed with status: " + clientResponse.statusCode()));
                                })
                )
                .bodyToMono(Map.class) // Or specific YouTube DTO
                .flatMapMany(this::parseYouTubeResponse) // This should return Flux<FeedPost>
                // === Explicit typing within flatMap ===
                .flatMap(feedPostObject -> { // Use a generic name first
                    // Explicitly check and cast the object from the upstream Flux
                    if (!(feedPostObject instanceof FeedPost)) {
                        log.warn("YouTube: Object in stream is not a FeedPost: {}", feedPostObject.getClass().getName());
                        return Mono.empty();
                    }
                    FeedPost feedPost = (FeedPost) feedPostObject; // Cast to FeedPost

                    // Check link *before* DB call (it should be set by parseYouTubeResponse)
                    if (feedPost.getLink() == null) {
                        log.warn("YouTube: Skipping post with null link before DB check.");
                        return Mono.empty();
                    }

                    // Check DB for duplicates
                    return Mono.fromCallable(() -> feedPostRepository.findByLink(feedPost.getLink())) // Use feedPost.getLink()
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(optionalPostObject -> { // Use generic name first
                                // Explicitly check and cast the object from the Mono result
                                if (!(optionalPostObject instanceof Optional)) {
                                    log.error("YouTube: Repository call did not return Optional: {}", optionalPostObject.getClass().getName());
                                    return Mono.empty();
                                }
                                @SuppressWarnings("unchecked")
                                Optional<FeedPost> optionalPost = (Optional<FeedPost>) optionalPostObject; // Cast to Optional<FeedPost>

                                if (optionalPost.isPresent()) { // Now use the typed variable
                                    log.debug("YouTube: Post already exists (blocking repo), filtering out: {}", feedPost.getLink());
                                    return Mono.empty();
                                } else {
                                    log.debug("YouTube: Post is new (blocking repo), keeping: {}", feedPost.getLink());
                                    return Mono.just(feedPost); // Return the original FeedPost object
                                }
                            });
                }) // <<< End of flatMap for repo check
                // === RESTORED Error Handling ===
                .doOnError(Throwable.class, error -> { // Use generic 'error' parameter name
                    // Explicitly check and cast 'error' before using methods
                    if (error instanceof Throwable) { // Check if it's actually a Throwable
                        Throwable throwableError = (Throwable) error; // Cast it
                        log.error("Error during YouTube processing chain for interest '{}': {}",
                                trimmedInterest,
                                throwableError.getMessage(), // Use casted variable's method
                                throwableError); // Log the casted variable
                    } else {
                        log.error("Non-Throwable error intercepted in YouTube doOnError for interest '{}': {}", trimmedInterest, error);
                    }
                })
                .onErrorResume(error -> Flux.empty()); // Simple resume
    }


    // --- COMPLETE Parsing Logic for YouTube API Response (with videoId added) ---
    private Flux<FeedPost> parseYouTubeResponse(Map<String, Object> response) {
        List<FeedPost> posts = new ArrayList<>();
        if (response == null) {
            log.warn("parseYouTubeResponse called with null response map.");
            return Flux.empty();
        }

        try {
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items == null) {
                log.warn("YouTube response missing 'items' array.");
                return Flux.empty();
            }

            log.debug("Processing {} items from YouTube response.", items.size());

            for (Map<String, Object> item : items) {
                Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
                Map<String, Object> idMap = (Map<String, Object>) item.get("id"); // Renamed to avoid conflict with FeedPost id field

                // --- Essential Checks ---
                if (idMap == null || !"youtube#video".equals(idMap.get("kind")) || idMap.get("videoId") == null || snippet == null) {
                    log.warn("Skipping non-video item or item with missing ID/snippet: {}", item);
                    continue;
                }

                String videoId = String.valueOf(idMap.get("videoId"));
                if (videoId.isEmpty()) { // Extra check
                    log.warn("Skipping item with empty videoId: {}", item);
                    continue;
                }
                // Construct the link (still useful for direct linking if needed)
                String videoLink = "https://www.youtube.com/watch?v=dQw4w9WgXcQ" + videoId;

                FeedPost post = new FeedPost();
                post.setPlatform("youtube");
                post.setLink(videoLink);
                // *** SET videoId ***
                post.setVideoId(videoId); // <<< This line was added/confirmed

                // --- Populate other fields from snippet ---
                post.setTitle(String.valueOf(snippet.getOrDefault("title", "Untitled")));
                post.setDescription(String.valueOf(snippet.getOrDefault("description", "")));
                if (post.getDescription().length() > 500) {
                    post.setDescription(post.getDescription().substring(0, 497) + "...");
                }
                post.setAuthor(String.valueOf(snippet.getOrDefault("channelTitle", "Unknown Channel")));

                // Thumbnails
                Map<String, Object> thumbnails = (Map<String, Object>) snippet.get("thumbnails");
                if (thumbnails != null) {
                    Map<String, Object> thumb = (Map<String, Object>) thumbnails.get("medium");
                    if (thumb == null) {
                        thumb = (Map<String, Object>) thumbnails.get("default");
                    }
                    if (thumb != null && thumb.get("url") instanceof String) {
                        post.setMediaUrl((String) thumb.get("url"));
                    }
                }

                // Timestamp
                Object publishedAtObj = snippet.get("publishedAt");
                if (publishedAtObj instanceof String) {
                    try {
                        post.setTimestamp(Instant.parse((String) publishedAtObj));
                    } catch (Exception pe) {
                        log.warn("Could not parse YouTube timestamp '{}' for videoId {}: {}", publishedAtObj, videoId, pe.getMessage());
                        post.setTimestamp(Instant.now()); // Fallback
                    }
                } else {
                    log.warn("Missing or invalid 'publishedAt' field for videoId {}", videoId);
                    post.setTimestamp(Instant.now()); // Fallback
                }

                post.setFetchedAt(Instant.now());

                log.debug("Successfully parsed YouTube video: {}", post.getLink());
                posts.add(post); // Add the fully populated post
            }
        } catch (ClassCastException | NullPointerException e) {
            log.error("Error parsing YouTube response structure: {}", e.getMessage(), e);
            return Flux.error(new RuntimeException("Failed to parse YouTube response structure", e));
        } catch (Exception e) {
            log.error("Unexpected error parsing YouTube response: {}", e.getMessage(), e);
            return Flux.error(new RuntimeException("Unexpected error parsing YouTube response", e));
        }

        log.info("Finished parsing YouTube response. {} posts extracted.", posts.size());
        return Flux.fromIterable(posts);
    }
}
