package com.feedfusion2.config; // Adjust package if needed

import com.feedfusion2.service.RedditAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    @Value("${reddit.user.agent}")
    private String redditUserAgent;

    @Value("${reddit.url.base}") // Use oauth.reddit.com for API calls
    private String redditBaseUrl;

    // Inject the AuthService
    private final RedditAuthService redditAuthService;

    public WebClientConfig(RedditAuthService redditAuthService) {
        this.redditAuthService = redditAuthService;
    }

    @Bean
    @Qualifier("redditWebClient") // Qualify this specific client
    public WebClient redditWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(redditBaseUrl) // Target oauth.reddit.com
                .defaultHeader(HttpHeaders.USER_AGENT, redditUserAgent) // Still required
                .filter(oauthFilter()) // Apply the OAuth filter
                .build();
    }

    // Filter to add the Authorization header
    private ExchangeFilterFunction oauthFilter() {
        return (request, next) -> redditAuthService.getValidAccessTokenMono()
                .flatMap(token -> {
                    log.debug("Adding Reddit Auth token to request: {}", request.url());
                    ClientRequest filteredRequest = ClientRequest.from(request)
                            .headers(headers -> headers.setBearerAuth(token))
                            .build();
                    return next.exchange(filteredRequest);
                })
                .switchIfEmpty(Mono.defer(() -> { // Handle case where token couldn't be obtained
                    log.error("Failed to obtain Reddit access token for request: {}", request.url());
                    // You might want to propagate a specific error or return an empty/error response from filter
                    // For simplicity, let the request proceed without auth, likely failing later
                    // Or return Mono.error(new RuntimeException("Cannot obtain Reddit token"));
                    return next.exchange(request);
                }));
    }


    // YouTube WebClient remains the same
    @Bean
    @Qualifier("youtubeWebClient")
    public WebClient youtubeWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://www.googleapis.com/youtube/v3")
                .build();
    }
}