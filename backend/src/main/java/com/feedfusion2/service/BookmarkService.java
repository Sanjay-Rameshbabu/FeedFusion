package com.feedfusion2.service; // Ensure package is correct

import com.feedfusion2.model.FeedPost;
import com.feedfusion2.model.User;
import com.feedfusion2.repository.FeedPostRepository; // Assuming blocking repository
import com.feedfusion2.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Or a custom exception
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookmarkService {

    private static final Logger log = LoggerFactory.getLogger(BookmarkService.class);

    private final UserRepository userRepository; // Blocking repository
    private final FeedPostRepository feedPostRepository; // Blocking repository

    @Autowired
    public BookmarkService(UserRepository userRepository, FeedPostRepository feedPostRepository) {
        this.userRepository = userRepository;
        this.feedPostRepository = feedPostRepository;
    }

    /**
     * Adds a post ID to a user's bookmarks.
     * @param userId ID of the user.
     * @param postId ID of the FeedPost to bookmark.
     * @return Mono signaling completion or error.
     */
    public Mono<Void> addBookmark(String userId, String postId) {
        log.debug("Attempting to add bookmark. UserID: {}, PostID: {}", userId, postId);
        // Wrap blocking calls for reactive context
        return Mono.fromCallable(() -> {
                    // Find the user (blocking)
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
                    // Add the bookmark (modifies the set in memory)
                    user.addBookmark(postId);
                    // Save the updated user (blocking)
                    userRepository.save(user);
                    log.info("Bookmark added successfully for UserID: {}, PostID: {}", userId, postId);
                    return null; // Return null for Void Mono
                })
                .subscribeOn(Schedulers.boundedElastic()) // Schedule blocking operations
                .then(); // Convert to Mono<Void>
    }

    /**
     * Removes a post ID from a user's bookmarks.
     * @param userId ID of the user.
     * @param postId ID of the FeedPost to unbookmark.
     * @return Mono signaling completion or error.
     */
    public Mono<Void> removeBookmark(String userId, String postId) {
        log.debug("Attempting to remove bookmark. UserID: {}, PostID: {}", userId, postId);
        // Wrap blocking calls
        return Mono.fromCallable(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
                    user.removeBookmark(postId);
                    userRepository.save(user);
                    log.info("Bookmark removed successfully for UserID: {}, PostID: {}", userId, postId);
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * Retrieves the bookmarked FeedPost objects for a given user.
     * @param userId ID of the user.
     * @return Flux emitting the bookmarked FeedPost objects.
     */
    public Flux<FeedPost> getBookmarkedPosts(String userId) {
        log.debug("Attempting to retrieve bookmarks for UserID: {}", userId);
        // Wrap blocking calls
        return Mono.fromCallable(() -> userRepository.findById(userId)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId))
                )
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(user -> { // Switch to Flux processing
                    Set<String> bookmarkedIds = user.getBookmarkedPostIds();
                    if (bookmarkedIds == null || bookmarkedIds.isEmpty()) {
                        log.debug("User {} has no bookmarks.", userId);
                        return Flux.empty(); // No IDs, return empty Flux
                    }
                    log.debug("User {} has {} bookmarks. Fetching posts...", userId, bookmarkedIds.size());
                    // Fetch all posts matching the bookmarked IDs (blocking)
                    // Wrap the blocking call
                    return Mono.fromCallable(() -> feedPostRepository.findAllById(bookmarkedIds))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMapMany(Flux::fromIterable); // Convert the resulting List<FeedPost> to Flux<FeedPost>
                })
                .doOnError(e -> log.error("Error retrieving bookmarked posts for UserID {}: {}", userId, e.getMessage()));
    }

    /**
     * Retrieves only the IDs of bookmarked posts for a given user.
     * Useful for the frontend to quickly check which posts are bookmarked.
     * @param userId ID of the user.
     * @return Mono containing a Set of bookmarked post IDs.
     */
    public Mono<Set<String>> getBookmarkedPostIds(String userId) {
        log.debug("Attempting to retrieve bookmark IDs for UserID: {}", userId);
        return Mono.fromCallable(() -> userRepository.findById(userId)
                        .map(User::getBookmarkedPostIds) // Get the set of IDs
                        .orElse(Collections.emptySet()) // Return empty set if user not found or has no bookmarks
                )
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("Error retrieving bookmark IDs for UserID {}: {}", userId, e.getMessage()));
    }
}
