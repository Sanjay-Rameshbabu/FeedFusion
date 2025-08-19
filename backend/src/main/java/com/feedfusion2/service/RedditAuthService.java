package com.feedfusion2.service; // Adjust package if needed

import com.feedfusion2.dto.AccessToken;
import com.feedfusion2.dto.RedditTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry; // Import Retry

import java.time.Duration; // Import Duration
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class RedditAuthService {

    private static final Logger log = LoggerFactory.getLogger(RedditAuthService.class);

    private final WebClient authWebClient; // Dedicated WebClient for auth calls
    private final String clientId;
    private final String clientSecret;
    private final String username;
    private final String password;
    private final String tokenUrl;
    private final String basicAuthHeader;
    private final String redditUserAgent;


    // Use AtomicReference for thread-safe updates to the token holder
    private final AtomicReference<AccessToken> currentToken = new AtomicReference<>();
    private final ReentrantLock refreshLock = new ReentrantLock(); // Lock to prevent concurrent refreshes

    public RedditAuthService(
            WebClient.Builder webClientBuilder,
            @Value("${reddit.client.id}") String clientId,
            @Value("${reddit.client.secret}") String clientSecret,
            @Value("${reddit.username}") String username,
            @Value("${reddit.password}") String password,
            @Value("${reddit.url.access_token}") String tokenUrl,
            @Value("${reddit.user.agent}") String redditUserAgent
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.username = username;
        this.password = password;
        this.tokenUrl = tokenUrl;
        this.redditUserAgent = redditUserAgent;

        // Create Basic Auth header value (ClientId:ClientSecret)
        this.basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

        // Build a dedicated WebClient for authentication requests
        this.authWebClient = webClientBuilder
                .baseUrl(tokenUrl) // Base URL is just the token endpoint for this client
                .defaultHeader(HttpHeaders.USER_AGENT, redditUserAgent) // User-Agent needed for auth too
                .build();

        log.info("RedditAuthService initialized.");
        // Initial token fetch on startup (optional, makes first API call faster)
        // Consider doing this in @PostConstruct or ApplicationRunner if needed immediately
        // getValidAccessTokenMono().subscribe(); // Fire-and-forget initial fetch
    }

    /**
     * Gets a valid access token, refreshing if necessary.
     * Returns a Mono that emits the token string.
     */
    public Mono<String> getValidAccessTokenMono() {
        AccessToken token = currentToken.get();
        if (token != null && !token.isExpired()) {
            log.debug("Returning cached Reddit access token.");
            return Mono.just(token.getToken());
        }
        // Token is null or expired, attempt refresh
        return fetchAndCacheNewToken();
    }


    private Mono<String> fetchAndCacheNewToken() {
        // Prevent multiple threads refreshing simultaneously
        if (refreshLock.tryLock()) {
            try {
                log.info("Attempting to fetch new Reddit access token...");
                AccessToken existingToken = currentToken.get();
                // Double-check if another thread refreshed it while waiting for the lock
                if (existingToken != null && !existingToken.isExpired()) {
                    log.debug("Token was refreshed by another thread while waiting for lock.");
                    return Mono.just(existingToken.getToken());
                }


                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                formData.add("grant_type", "password");
                formData.add("username", username);
                formData.add("password", password);
                // Reddit might sometimes require scope, '*' is common for script apps
                // formData.add("scope", "*");

                return this.authWebClient.post()
                        .uri("") // URI is empty because base URL is the full token endpoint
                        .header(HttpHeaders.AUTHORIZATION, this.basicAuthHeader)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(BodyInserters.fromFormData(formData))
                        .retrieve()
                        .onStatus(status -> status.isError(), response ->
                                response.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            log.error("Error fetching Reddit token: Status={}, Body={}", response.statusCode(), body);
                                            return Mono.error(new RuntimeException("Failed to fetch Reddit token: " + response.statusCode()));
                                        })
                        )
                        .bodyToMono(RedditTokenResponse.class)
                        .flatMap(tokenResponse -> {
                            if (tokenResponse.getAccessToken() == null || tokenResponse.getAccessToken().isEmpty()) {
                                log.error("Received null or empty access token from Reddit.");
                                return Mono.error(new RuntimeException("Received null or empty access token"));
                            }
                            log.info("Successfully fetched new Reddit access token. Expires in: {}s", tokenResponse.getExpiresIn());
                            AccessToken newToken = new AccessToken(tokenResponse.getAccessToken(), tokenResponse.getExpiresIn());
                            currentToken.set(newToken); // Atomically update the token
                            return Mono.just(newToken.getToken());
                        })
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)) // Retry on transient errors
                                .filter(throwable -> !(throwable instanceof RuntimeException && throwable.getMessage().contains("Failed to fetch"))) // Don't retry auth failures
                                .doBeforeRetry(retrySignal -> log.warn("Retrying token fetch attempt #{}: {}", retrySignal.totalRetries() + 1, retrySignal.failure().getMessage()))
                        )
                        .doOnError(e -> log.error("Failed to fetch Reddit token after retries: {}", e.getMessage()));

            } finally {
                refreshLock.unlock(); // Ensure lock is always released
            }
        } else {
            // Another thread is already refreshing, wait briefly and retry getting token
            log.debug("Refresh lock held by another thread, delaying and retrying getAccessTokenMono");
            return Mono.delay(Duration.ofMillis(200)).flatMap(l -> getValidAccessTokenMono());

        }
    }
}