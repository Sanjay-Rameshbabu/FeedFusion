////package com.feedfusion2.service; // Ensure package/imports match your project
////
////import com.feedfusion2.model.FeedPost;
////import com.feedfusion2.repository.FeedPostRepository;
////import org.slf4j.Logger;
////import org.slf4j.LoggerFactory;
////import org.springframework.beans.factory.annotation.Autowired;
////import org.springframework.data.domain.Sort;
////import org.springframework.stereotype.Service;
////import reactor.core.publisher.Flux;
////import reactor.core.publisher.Mono;
////import reactor.core.scheduler.Schedulers; // Import Schedulers
////
////import java.time.Instant;
////import java.util.List;
////import java.util.Comparator; // Keep if you plan to use it later
////
////@Service
////public class FeedService {
////
////    private static final Logger log = LoggerFactory.getLogger(FeedService.class);
////    private final RedditService redditService;
////    private final YouTubeService youtubeService;
////    // Using blocking MongoRepository
////    private final FeedPostRepository feedPostRepository;
////
////    @Autowired
////    public FeedService(RedditService redditService, YouTubeService youtubeService, FeedPostRepository feedPostRepository) {
////        this.redditService = redditService;
////        this.youtubeService = youtubeService;
////        this.feedPostRepository = feedPostRepository;
////    }
////
////    public Mono<List<FeedPost>> getAggregatedFeed(List<String> interests) {
////        log.info("Aggregating feed for interests: {}", interests);
////        if (interests == null || interests.isEmpty()) {
////            return Mono.just(List.of());
////        }
////
////        // Fetch new posts reactively
////        Flux<FeedPost> fetchedPostsFlux = Flux.fromIterable(interests)
////                .flatMap(interest -> Flux.merge(
////                        redditService.fetchRedditPosts(interest),
////                        youtubeService.fetchYouTubeVideos(interest)
////                ));
////
////        // Process fetched posts: save new ones, then return all
////        return fetchedPostsFlux
////                .collectList()
////                .flatMap(newPosts -> {
////                    if (!newPosts.isEmpty()) {
////                        log.info("Attempting to save {} new posts to database.", newPosts.size());
////                        // === Correctly handle blocking save within flatMap ===
////                        return Flux.fromIterable(newPosts)
////                                // Replace method reference with lambda + fromCallable + subscribeOn
////                                .flatMap(postToSave ->
////                                        Mono.fromCallable(() -> {
////                                                    log.debug("Saving post: {}", postToSave.getLink());
////                                                    return feedPostRepository.save(postToSave); // Blocking call
////                                                })
////                                                .subscribeOn(Schedulers.boundedElastic()) // Schedule blocking call
////                                                .doOnError(e -> log.error("Error saving post {}: {}", postToSave.getLink(), e.getMessage(), e))
////                                )
////                                .collectList() // Collect results of save operations (optional)
////                                .doOnSuccess(saved -> log.info("Finished save operations for {} posts.", saved.size()))
////                                .then(Mono.just(newPosts)); // Continue chain, passing original newPosts list
////                        // Using .then() ensures we proceed even if saving had issues (errors handled by doOnError/log)
////                        // and we always fetch the full list afterwards.
////                    } else {
////                        log.info("No new posts fetched, skipping save operation.");
////                        return Mono.just(List.<FeedPost>of()); // Return empty list wrapped in Mono
////                    }
////                })
////                .flatMap(listUsedForFlowControl -> { // Variable name clarifies its purpose
////                    // Always fetch the latest full list from DB after potential saves
////                    log.info("Retrieving all cached posts sorted by timestamp.");
////                    // === Wrap and schedule the blocking findAll call ===
////                    return Mono.fromCallable(() ->
////                                    feedPostRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"))
////                            )
////                            .subscribeOn(Schedulers.boundedElastic()); // Schedule this blocking call too
////                })
////                .onErrorResume(e -> {
////                    log.error("Error in getAggregatedFeed processing chain: {}", e.getMessage(), e);
////                    return Mono.just(List.of()); // Return empty list on error
////                });
////    }
////
////
////    public Mono<List<FeedPost>> searchAndFilterFeed(String platform, String keyword) {
////        log.info("Searching feed with platform='{}' and keyword='{}'", platform, keyword);
////        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
////        // Defensive trimming and lowercasing for platform
////        String effectivePlatform = (platform != null && !platform.trim().isEmpty() && !platform.equalsIgnoreCase("all"))
////                ? platform.trim().toLowerCase() : null;
////        String effectiveKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
////
////
////        // === Wrap the blocking DB call logic AND schedule it ===
////        return Mono.fromCallable(() -> {
////                    log.debug("Executing blocking search: platform={}, keyword={}", effectivePlatform, effectiveKeyword);
////                    if (effectivePlatform != null && effectiveKeyword != null) {
////                        return feedPostRepository.findByPlatformAndKeyword(effectivePlatform, effectiveKeyword, sort);
////                    } else if (effectivePlatform != null) {
////                        return feedPostRepository.findByPlatform(effectivePlatform, sort);
////                    } else if (effectiveKeyword != null) {
////                        return feedPostRepository.findByKeyword(effectiveKeyword, sort);
////                    } else {
////                        return feedPostRepository.findAll(sort);
////                    }
////                })
////                .subscribeOn(Schedulers.boundedElastic()) // <-- Schedule the blocking query execution
////                .doOnError(e -> log.error("Error during searchAndFilterFeed DB query: {}", e.getMessage(), e))
////                .onErrorReturn(List.of()); // Return empty list if DB query fails
////    }
////}
////Above code is not showing reddit post
//package com.feedfusion2.service; // Ensure package/imports match your project
//
//import com.feedfusion2.model.FeedPost;
//import com.feedfusion2.repository.FeedPostRepository;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Sort;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.core.scheduler.Schedulers; // Import Schedulers
//
//import java.util.List;
//// import java.util.Comparator; // Keep if needed later
//
//@Service
//public class FeedService {
//
//    private static final Logger log = LoggerFactory.getLogger(FeedService.class);
//    private final RedditService redditService;
//    private final YouTubeService youtubeService;
//    // Using blocking MongoRepository
//    private final FeedPostRepository feedPostRepository;
//
//    @Autowired
//    public FeedService(RedditService redditService, YouTubeService youtubeService, FeedPostRepository feedPostRepository) {
//        this.redditService = redditService;
//        this.youtubeService = youtubeService;
//        this.feedPostRepository = feedPostRepository;
//    }
//
//    public Mono<List<FeedPost>> getAggregatedFeed(List<String> interests) {
//        log.info("Aggregating feed for interests: {}", interests);
//        if (interests == null || interests.isEmpty()) {
//            return Mono.just(List.of());
//        }
//
//        // Fetch posts reactively, processing each interest individually
//        Flux<FeedPost> fetchedPostsFlux = Flux.fromIterable(interests)
//                .flatMap(interest -> {
//                    // Log which interest is being processed
//                    log.debug("Processing interest: {}", interest);
//
//                    // --- Fetch from Reddit for this interest ---
//                    Flux<FeedPost> redditFlux = redditService.fetchRedditPosts(interest)
//                            // Log posts successfully emitted by RedditService (after its internal filtering)
//                            .doOnNext(post -> log.debug("[Reddit] Emitting post for interest '{}': {}", interest, post.getLink()))
//                            // Log errors specific to this Reddit fetch attempt
//                            .doOnError(e -> log.error("[Reddit] Error fetching posts for interest '{}': {}", interest, e.getMessage()))
//                            // If Reddit fetch fails for this interest, log warning and continue with empty, don't break the whole flow
//                            .onErrorResume(e -> {
//                                log.warn("[Reddit] Resuming with empty stream after error for interest '{}'.", interest);
//                                return Flux.empty(); // Return empty Flux for this source on error
//                            });
//
//                    // --- Fetch from YouTube for this interest ---
//                    Flux<FeedPost> youtubeFlux = youtubeService.fetchYouTubeVideos(interest)
//                            // Log posts successfully emitted by YouTubeService (after its internal filtering)
//                            .doOnNext(post -> log.debug("[YouTube] Emitting post for interest '{}': {}", interest, post.getLink()))
//                            // Log errors specific to this YouTube fetch attempt
//                            .doOnError(e -> log.error("[YouTube] Error fetching posts for interest '{}': {}", interest, e.getMessage()))
//                            // If YouTube fetch fails for this interest, log warning and continue with empty
//                            .onErrorResume(e -> {
//                                log.warn("[YouTube] Resuming with empty stream after error for interest '{}'.", interest);
//                                return Flux.empty(); // Return empty Flux for this source on error
//                            });
//
//                    // Merge the results for THIS interest. If one source failed, the other can still proceed.
//                    return Flux.merge(redditFlux, youtubeFlux)
//                            .doOnComplete(() -> log.debug("Finished merging Reddit/YouTube sources for interest: {}", interest));
//                }); // End of flatMap per interest
//
//        // --- Process all fetched posts across all interests ---
//        return fetchedPostsFlux
//                .collectList() // Collect all posts that were successfully emitted from all interests
//                .flatMap(newlyFetchedPosts -> {
//                    log.info("Total potentially new posts fetched across all interests: {}", newlyFetchedPosts.size());
//                    if (!newlyFetchedPosts.isEmpty()) {
//                        log.info("Attempting to save {} posts to database.", newlyFetchedPosts.size());
//                        // Save each post individually, handling blocking calls and potential unique key errors
//                        return Flux.fromIterable(newlyFetchedPosts)
//                                .flatMap(postToSave ->
//                                        Mono.fromCallable(() -> {
//                                                    log.debug("Saving post via FeedService: {}", postToSave.getLink());
//                                                    // save() performs insert or update. Unique index protects against duplicates.
//                                                    return feedPostRepository.save(postToSave); // Blocking call
//                                                })
//                                                .subscribeOn(Schedulers.boundedElastic()) // Schedule blocking call
//                                                // Log errors during save (e.g., unique key violation if index exists)
//                                                .doOnError(e -> log.error("Error saving post {} via FeedService: {}", postToSave.getLink(), e.getMessage()))
//                                                // If save fails for one post, log it but continue with others
//                                                .onErrorResume(e -> {
//                                                    log.warn("Failed to save post {}, continuing with others.", postToSave.getLink());
//                                                    return Mono.empty(); // Skip this post on save error
//                                                })
//                                )
//                                .collectList() // Collect successfully saved/updated posts
//                                .doOnSuccess(savedList -> log.info("Finished save operations via FeedService. {} posts successfully saved/updated.", savedList.size()))
//                                // Proceed to fetch the final list regardless of save outcomes
//                                .thenReturn("Save process completed"); // Signal completion
//                    } else {
//                        log.info("No new posts fetched from any source, skipping save operation.");
//                        return Mono.just("No posts to save"); // Signal skip
//                    }
//                })
//                .flatMap(saveResultMessage -> {
//                    // Always retrieve the full, sorted list from the DB as the final result
//                    log.info("Retrieving final feed list from DB (all posts sorted by timestamp).");
//                    return Mono.fromCallable(() ->
//                                    feedPostRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"))
//                            )
//                            .subscribeOn(Schedulers.boundedElastic()); // Schedule blocking call
//                })
//                .doOnError(e -> log.error("Error retrieving final feed list from DB: {}", e.getMessage(), e)) // Log error during final fetch
//                .onErrorReturn(List.of()); // Return empty list if final DB fetch fails
//    }
//
//
//    public Mono<List<FeedPost>> searchAndFilterFeed(String platform, String keyword) {
//        log.info("Searching feed with platform='{}' and keyword='{}'", platform, keyword);
//        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
//        String effectivePlatform = (platform != null && !platform.trim().isEmpty() && !platform.equalsIgnoreCase("all"))
//                ? platform.trim().toLowerCase() : null;
//        String effectiveKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
//
//        // Wrap the blocking DB call logic AND schedule it
//        return Mono.fromCallable(() -> {
//                    log.debug("Executing blocking search DB query: platform={}, keyword={}", effectivePlatform, effectiveKeyword);
//                    if (effectivePlatform != null && effectiveKeyword != null) {
//                        return feedPostRepository.findByPlatformAndKeyword(effectivePlatform, effectiveKeyword, sort);
//                    } else if (effectivePlatform != null) {
//                        return feedPostRepository.findByPlatform(effectivePlatform, sort);
//                    } else if (effectiveKeyword != null) {
//                        return feedPostRepository.findByKeyword(effectiveKeyword, sort);
//                    } else {
//                        return feedPostRepository.findAll(sort);
//                    }
//                })
//                .subscribeOn(Schedulers.boundedElastic()) // Schedule the blocking query execution
//                .doOnSuccess(results -> {
//                    if (results != null) {
//                        log.debug("Search DB query completed successfully, found {} results.", results.size());
//                    } else {
//                        log.debug("Search DB query completed successfully with null result."); // Should not happen for List
//                    }
//                })
//                .doOnError(e -> log.error("Error during searchAndFilterFeed DB query: {}", e.getMessage(), e))
//                .onErrorReturn(List.of()); // Return empty list if DB query fails
//    }
//}

package com.feedfusion2.service; // Ensure package/imports match your project

import com.feedfusion2.model.FeedPost;
import com.feedfusion2.repository.FeedPostRepository; // Assuming blocking repository
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // For checking empty strings
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.function.Predicate;

@Service
public class FeedService {

    private static final Logger log = LoggerFactory.getLogger(FeedService.class);
    private final RedditService redditService; // Assuming these return Flux<FeedPost>
    private final YouTubeService youtubeService; // Assuming these return Flux<FeedPost>
    private final FeedPostRepository feedPostRepository; // Assuming blocking repository

    @Autowired
    public FeedService(RedditService redditService, YouTubeService youtubeService, FeedPostRepository feedPostRepository) {
        this.redditService = redditService;
        this.youtubeService = youtubeService;
        this.feedPostRepository = feedPostRepository;
    }

    /**
     * Fetches posts based on interests, saves new ones, retrieves all from DB,
     * and then applies platform and keyword filters.
     *
     * @param platform Optional platform filter.
     * @param keyword Optional keyword filter.
     * @param interests List of interests to fetch posts for.
     * @return Flux emitting filtered FeedPost objects.
     */
    public Flux<FeedPost> getFilteredFeed(String platform, String keyword, List<String> interests) {
        log.info("Starting filtered feed retrieval for platform: '{}', keyword: '{}', interests: {}", platform, keyword, interests);

        // 1. Fetch and Save Logic (based on interests)
        Mono<String> fetchAndSaveMono = fetchAndSaveByInterests(interests);

        // 2. Retrieve All from DB (after fetch/save completes)
        Mono<List<FeedPost>> allPostsFromDbMono = fetchAndSaveMono
                .flatMap(result -> {
                    log.info("Retrieving all posts from DB after fetch/save step ({})", result);
                    return Mono.fromCallable(() ->
                                    feedPostRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"))
                            )
                            .subscribeOn(Schedulers.boundedElastic());
                })
                .doOnError(e -> log.error("Error retrieving posts from DB: {}", e.getMessage(), e))
                .onErrorReturn(List.of()); // Return empty list if DB fetch fails

        // 3. Apply Filters to the retrieved list
        return allPostsFromDbMono
                .flatMapMany(allPosts -> {
                    log.debug("Applying filters to {} posts retrieved from DB.", allPosts.size());
                    // Create predicates for filtering
                    Predicate<FeedPost> platformFilter = post -> !StringUtils.hasText(platform) // No platform filter OR
                            || (post.getPlatform() != null && post.getPlatform().equalsIgnoreCase(platform.trim())); // Platform matches (case-insensitive)

                    Predicate<FeedPost> keywordFilter = post -> !StringUtils.hasText(keyword) // No keyword filter OR
                            || (post.getTitle() != null && post.getTitle().toLowerCase().contains(keyword.trim().toLowerCase())) // Title contains keyword OR
                            || (post.getDescription() != null && post.getDescription().toLowerCase().contains(keyword.trim().toLowerCase())); // Description contains keyword

                    // Apply filters using Flux stream operations
                    return Flux.fromIterable(allPosts)
                            .filter(platformFilter)
                            .filter(keywordFilter)
                            .doOnComplete(() -> log.info("Filtering complete for platform: '{}', keyword: '{}'", platform, keyword));
                });
    }


    /**
     * Helper method to fetch posts by interests and save them.
     * Encapsulates the logic previously in getAggregatedFeed.
     *
     * @param interests List of interests.
     * @return A Mono signaling completion ("Save process completed" or "No posts to save").
     */
    private Mono<String> fetchAndSaveByInterests(List<String> interests) {
        log.debug("Fetching and saving posts for interests: {}", interests);
        if (interests == null || interests.isEmpty()) {
            log.debug("No interests provided, skipping fetch and save.");
            return Mono.just("No interests provided");
        }

        // Fetch posts reactively
        Flux<FeedPost> fetchedPostsFlux = Flux.fromIterable(interests)
                .flatMap(interest -> {
                    log.debug("Fetching sources for interest: {}", interest);
                    Flux<FeedPost> redditFlux = redditService.fetchRedditPosts(interest)
                            .doOnError(e -> log.error("[Reddit] Error fetching for interest '{}': {}", interest, e.getMessage()))
                            .onErrorResume(e -> Flux.empty()); // Continue if Reddit fails

                    Flux<FeedPost> youtubeFlux = youtubeService.fetchYouTubeVideos(interest)
                            .doOnError(e -> log.error("[YouTube] Error fetching for interest '{}': {}", interest, e.getMessage()))
                            .onErrorResume(e -> Flux.empty()); // Continue if YouTube fails

                    return Flux.merge(redditFlux, youtubeFlux);
                });

        // Process fetched posts: save new ones
        return fetchedPostsFlux
                .collectList()
                .flatMap(newlyFetchedPosts -> {
                    if (!newlyFetchedPosts.isEmpty()) {
                        log.info("Attempting to save {} potentially new posts.", newlyFetchedPosts.size());
                        return Flux.fromIterable(newlyFetchedPosts)
                                .flatMap(postToSave ->
                                        Mono.fromCallable(() -> feedPostRepository.save(postToSave)) // Blocking save
                                                .subscribeOn(Schedulers.boundedElastic()) // Schedule blocking call
                                                .doOnError(e -> log.error("Error saving post {}: {}", postToSave.getLink(), e.getMessage()))
                                                .onErrorResume(e -> Mono.empty()) // Skip post on save error
                                )
                                .then(Mono.just("Save process completed")); // Signal completion after all saves attempted
                    } else {
                        log.info("No new posts fetched, skipping save.");
                        return Mono.just("No posts to save"); // Signal skip
                    }
                });
    }

    // Consider removing getAggregatedFeed and searchAndFilterFeed methods
    // if they are no longer directly used by the controller.

}
