//package com.feedfusion2.controller;
//
//import com.feedfusion2.dto.InterestsRequest;
//import com.feedfusion2.model.FeedPost;
//import com.feedfusion2.service.FeedService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/feed")
//// Note: CORS is handled globally by CorsConfig, but @CrossOrigin could be used here too.
//public class FeedController {
//
//    private final FeedService feedService;
//
//    @Autowired
//    public FeedController(FeedService feedService) {
//        this.feedService = feedService;
//    }
//
//    @PostMapping("/by-interests")
//    public Mono<ResponseEntity<List<FeedPost>>> getFeedByInterests(@RequestBody InterestsRequest request) {
//        if (request == null || request.getInterests() == null || request.getInterests().isEmpty()) {
//            // Return Bad Request if interests list is missing or empty
//            return Mono.just(ResponseEntity.badRequest().body(List.of()));
//        }
//        return feedService.getAggregatedFeed(request.getInterests())
//                .map(ResponseEntity::ok) // Wrap the list in ResponseEntity.ok()
//                .defaultIfEmpty(ResponseEntity.ok(List.of())); // Return OK with empty list if no posts found
//    }
//
//    @GetMapping("/search")
//    public Mono<ResponseEntity<List<FeedPost>>> searchFeed(
//            @RequestParam(required = false) String platform,
//            @RequestParam(required = false) String keyword) {
//        // Allow null/empty platform and keyword
//        return feedService.searchAndFilterFeed(platform, keyword)
//                .map(ResponseEntity::ok)
//                .defaultIfEmpty(ResponseEntity.ok(List.of()));
//    }
//}

package com.feedfusion2.controller; // Ensure package name is correct

// Import necessary classes
import com.feedfusion2.model.FeedPost; // Assuming this is your DTO/Model for posts
import com.feedfusion2.service.FeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux; // FeedService likely returns Flux
import reactor.core.publisher.Mono; // Using Reactor

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feed") // Base path for feed-related endpoints
public class FeedController {

    private final FeedService feedService;

    @Autowired
    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    /**
     * Fetches the aggregated feed based on optional filters.
     *
     * @param platform  Optional platform filter (e.g., "reddit", "youtube").
     * @param keyword   Optional keyword filter for searching titles/descriptions.
     * @param interests Optional comma-separated string of interests.
     * @return A Mono containing a ResponseEntity with a list of FeedPost objects.
     */
    @GetMapping // Handles GET requests to /api/feed
    public Mono<ResponseEntity<List<FeedPost>>> getFeed(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String interests // Accept interests as comma-separated string
    ) {
        // Convert comma-separated interests string to a List<String>
        // Handle null or empty string gracefully
        List<String> interestList = (interests != null && !interests.trim().isEmpty())
                ? Arrays.stream(interests.split(","))
                .map(String::trim) // Trim whitespace from each interest
                .filter(s -> !s.isEmpty()) // Filter out empty strings after split
                .collect(Collectors.toList())
                : Collections.emptyList(); // Use an empty list if no interests provided

        // Log the received parameters for debugging
        System.out.printf("Received feed request with platform: %s, keyword: %s, interests: %s%n",
                platform, keyword, interestList);

        // Call a single service method that handles all filtering logic
        // Assuming getFilteredFeed returns Flux<FeedPost>
        Flux<FeedPost> feedFlux = feedService.getFilteredFeed(platform, keyword, interestList);

        // Collect the Flux into a List and wrap in ResponseEntity inside a Mono
        return feedFlux
                .collectList() // Collect the Flux<FeedPost> from service into a Mono<List<FeedPost>>
                .map(ResponseEntity::ok) // Wrap the list in ResponseEntity.ok()
                .defaultIfEmpty(ResponseEntity.ok(List.of())); // Return OK with empty list if no posts found
    }

    // You can remove the old /by-interests and /search endpoints now
    // if they are no longer needed.

}
