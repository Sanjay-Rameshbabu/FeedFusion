package com.feedfusion2.controller; // Ensure package is correct

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.feedfusion2.dto.MessageResponse; // Assuming you have this DTO
import com.feedfusion2.model.FeedPost;
import com.feedfusion2.model.User; // Import User model if needed for ID extraction
import com.feedfusion2.repository.UserRepository; // Import UserRepository
import com.feedfusion2.service.BookmarkService;
// Removed UserDetailsServiceImpl import as it's not directly used here
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // For securing endpoints
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder; // Use reactive context
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
// --- V V V Add these imports V V V ---
import reactor.core.scheduler.Schedulers; // For scheduling blocking calls
import java.util.Collections; // For Collections.emptySet()
// --- ^ ^ ^ End of added imports ^ ^ ^ ---

import java.security.Principal; // Can also use Principal
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/bookmarks") // Base path for bookmark endpoints
public class BookmarkController {

    private static final Logger log = LoggerFactory.getLogger(BookmarkController.class);

    private final BookmarkService bookmarkService;
    private final UserRepository userRepository; // Inject UserRepository to get User ID

    @Autowired
    public BookmarkController(BookmarkService bookmarkService, UserRepository userRepository) {
        this.bookmarkService = bookmarkService;
        this.userRepository = userRepository;
        log.info("BookmarkController initialized."); // Log initialization
    }

    /**
     * Helper method to get the current authenticated user's ID reactively.
     * @return Mono containing the user ID, or Mono.error if not authenticated or user not found.
     */
    private Mono<String> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetails)) {
                        log.warn("User not authenticated in security context.");
                        return Mono.error(new IllegalStateException("User not authenticated"));
                    }
                    String username = ((UserDetails) authentication.getPrincipal()).getUsername();
                    log.debug("Authenticated username: {}", username);
                    // Find user ID based on username (blocking call needs adaptation)
                    return Mono.fromCallable(() -> {
                                log.debug("Looking up user ID for username: {}", username);
                                return userRepository.findByUsername(username)
                                        .map(User::getId) // Extract ID from User object
                                        .orElseThrow(() -> {
                                            log.warn("Authenticated user '{}' not found in database.", username);
                                            return new IllegalStateException("Authenticated user not found in database");
                                        });
                            })
                            .subscribeOn(Schedulers.boundedElastic()); // Schedule blocking call
                })
                .switchIfEmpty(Mono.error(new IllegalStateException("Authentication context is empty"))); // Handle empty context case
    }


    /**
     * Adds a bookmark for the authenticated user.
     * Expects postId in the request body e.g., { "postId": "someMongoId" }
     * @param requestBodyMono Mono containing the BookmarkRequest.
     * @return Mono<ResponseEntity>
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()") // Ensure user is logged in
    public Mono<ResponseEntity<MessageResponse>> addBookmark(@RequestBody Mono<BookmarkRequest> requestBodyMono) { // Use @RequestBody
        return getCurrentUserId()
                .flatMap(userId -> requestBodyMono
                        .flatMap(request -> {
                            String postId = request.getPostId();
                            if (postId == null || postId.trim().isEmpty()) {
                                log.warn("Add bookmark request failed for user {}: postId is empty", userId);
                                // Return 400 Bad Request inside the Mono
                                return Mono.just(ResponseEntity.badRequest().body(new MessageResponse("Error: postId cannot be empty")));
                            }
                            log.info("User {} adding bookmark for post {}", userId, postId);
                            // Call the service, then map success to ResponseEntity
                            return bookmarkService.addBookmark(userId, postId)
                                    .then(Mono.just(ResponseEntity.ok(new MessageResponse("Bookmark added successfully"))));
                        })
                        // Handle case where request body Mono is empty or invalid before service call
                        .switchIfEmpty(Mono.just(ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid request body"))))
                )
                .onErrorResume(e -> { // Catch errors from getCurrentUserId or addBookmark
                    log.error("Error adding bookmark: {}", e.getMessage());
                    HttpStatus status = (e instanceof IllegalStateException || e instanceof UsernameNotFoundException) ? HttpStatus.UNAUTHORIZED : HttpStatus.INTERNAL_SERVER_ERROR;
                    return Mono.just(ResponseEntity.status(status).body(new MessageResponse("Error adding bookmark: " + e.getMessage())));
                });
    }


    /**
     * Removes a bookmark for the authenticated user.
     * @param postId The ID of the post to remove from bookmarks (from path variable).
     * @return Mono<ResponseEntity>
     */
    @DeleteMapping("/{postId}")
    @PreAuthorize("isAuthenticated()") // Ensure user is logged in
    public Mono<ResponseEntity<MessageResponse>> removeBookmark(@PathVariable String postId) {
        return getCurrentUserId()
                .flatMap(userId -> {
                    log.info("User {} removing bookmark for post {}", userId, postId);
                    return bookmarkService.removeBookmark(userId, postId)
                            .then(Mono.just(ResponseEntity.ok(new MessageResponse("Bookmark removed successfully"))));
                })
                .onErrorResume(e -> { // Catch errors from getCurrentUserId or removeBookmark
                    log.error("Error removing bookmark for post {}: {}", postId, e.getMessage());
                    HttpStatus status = (e instanceof IllegalStateException || e instanceof UsernameNotFoundException) ? HttpStatus.UNAUTHORIZED : HttpStatus.INTERNAL_SERVER_ERROR;
                    return Mono.just(ResponseEntity.status(status).body(new MessageResponse("Error removing bookmark: " + e.getMessage())));
                });
    }

    /**
     * Gets all bookmarked FeedPost objects for the authenticated user.
     * @return Flux<FeedPost>
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()") // Ensure user is logged in
    public Flux<FeedPost> getBookmarks() {
        // Use flatMapMany to switch from Mono<String> (userId) to Flux<FeedPost>
        return getCurrentUserId()
                .doOnNext(userId -> log.info("Fetching bookmarks for user {}", userId))
                .flatMapMany(bookmarkService::getBookmarkedPosts) // Use method reference
                .doOnError(e -> log.error("Error retrieving bookmarks stream: {}", e.getMessage()));
        // Errors during individual post fetching within the service are handled there
    }

    /**
     * Gets only the IDs of bookmarked posts for the authenticated user.
     * @return Mono<ResponseEntity<Set<String>>>
     */
    @GetMapping("/ids")
    @PreAuthorize("isAuthenticated()") // Ensure user is logged in
    public Mono<ResponseEntity<Set<String>>> getBookmarkIds() {
        return getCurrentUserId()
                .doOnNext(userId -> log.info("Fetching bookmark IDs for user {}", userId))
                .flatMap(bookmarkService::getBookmarkedPostIds) // Use method reference
                .map(ResponseEntity::ok) // Wrap the Set in ResponseEntity
                .onErrorResume(e -> {
                    log.error("Error retrieving bookmark IDs: {}", e.getMessage());
                    // Return empty set with appropriate status on error
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.<String>emptySet()));
                });
    }

    // --- Simple DTO for the POST request body ---
    // Can be a static inner class or a separate file in the dto package
    private static class BookmarkRequest {
        private String postId;
        // Need getters/setters for Jackson deserialization
        public String getPostId() { return postId; }
        public void setPostId(String postId) { this.postId = postId; }
    }
    // --- End DTO ---
}

