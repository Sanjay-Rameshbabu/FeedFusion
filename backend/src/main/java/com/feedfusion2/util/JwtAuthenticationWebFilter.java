package com.feedfusion2.util; // Ensure this package name is correct

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
// --- V V V Ensure these REACTIVE imports are used V V V ---
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
// --- ^ ^ ^ Ensure these REACTIVE imports are used ^ ^ ^ ---
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder; // Reactive Context
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService; // Reactive Service
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

// --- Ensure NO imports from jakarta.servlet.* exist ---

@Component
public class JwtAuthenticationWebFilter implements WebFilter { // Implement reactive WebFilter

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationWebFilter.class);

    private final JwtUtil jwtUtil;
    private final ReactiveUserDetailsService reactiveUserDetailsService; // Use Reactive type

    @Autowired
    public JwtAuthenticationWebFilter(JwtUtil jwtUtil, ReactiveUserDetailsService reactiveUserDetailsService) {
        this.jwtUtil = jwtUtil;
        this.reactiveUserDetailsService = reactiveUserDetailsService;
        logger.info("JwtAuthenticationWebFilter initialized with dependencies.");
    }

    // --- Use REACTIVE filter method signature ---
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // --- Use REACTIVE filter method signature ---
        ServerHttpRequest request = exchange.getRequest();
        String jwt = parseJwt(request); // Use reactive request

        final String requestId = exchange.getRequest().getId();
        logger.debug("[{}] --- Reactive JwtAuthenticationWebFilter START for request: {}", requestId, request.getURI());
        logger.trace("[{}] Parsed JWT: {}", requestId, (jwt != null ? "[PRESENT]" : "[NULL]"));

        if (jwt != null && jwtUtil.validateJwtToken(jwt)) {
            logger.debug("[{}] JWT is valid.", requestId);
            String username = jwtUtil.getUsernameFromJwtToken(jwt);
            logger.debug("[{}] Username from JWT: {}", requestId, username);

            // Load UserDetails reactively
            return reactiveUserDetailsService.findByUsername(username)
                    .flatMap(userDetails -> { // If userDetails is found
                        logger.debug("[{}] UserDetails loaded for username: {}", requestId, userDetails.getUsername());
                        // Create the Authentication object
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        logger.debug("[{}] Authentication created: {}", requestId, authentication.isAuthenticated());

                        // Create a new SecurityContext with the Authentication
                        SecurityContext context = new SecurityContextImpl(authentication);
                        logger.debug("[{}] Authentication set in new SecurityContext.", requestId);

                        logger.debug("[{}] --- Reactive JwtAuthenticationWebFilter END (Authenticated) for request: {}", requestId, request.getURI());
                        // Continue the filter chain, attaching the SecurityContext to the reactive context
                        return chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));
                    })
                    .doOnError(e -> logger.error("[{}] Error loading user details or setting context: {}", requestId, e.getMessage(), e))
                    .onErrorResume(e -> { // If findByUsername fails or flatMap has issues
                        logger.warn("[{}] Proceeding without authentication due to error during user loading: {}", requestId, e.getMessage());
                        logger.debug("[{}] --- Reactive JwtAuthenticationWebFilter END (User Load Error) for request: {}", requestId, request.getURI());
                        return chain.filter(exchange); // Continue chain without authentication on error
                    });
        } else {
            // Log reason for not proceeding (no token or invalid token)
            if (jwt == null) { logger.trace("[{}] No JWT found in request header.", requestId); }
            else { logger.warn("[{}] JWT validation failed (check JwtUtil logs for reason).", requestId); }
            // If no valid token, just continue the chain without setting authentication
            logger.debug("[{}] --- Reactive JwtAuthenticationWebFilter END (No/Invalid JWT) for request: {}", requestId, request.getURI());
            return chain.filter(exchange);
        }
    }

    // Helper method using reactive request
    private String parseJwt(ServerHttpRequest request) {
        String headerAuth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        // logger.trace("Authorization Header: {}", headerAuth); // Use trace for potentially sensitive data

        // Check if the header exists and starts with "Bearer "
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            // Extract the token part (after "Bearer ")
            return headerAuth.substring(7);
        }
        // Return null if no valid Bearer token found
        return null;
    }
}
