package com.feedfusion2.config; // Ensure package name is correct

// Imports...
import com.feedfusion2.util.JwtAuthenticationWebFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity // Keep for @PreAuthorize
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final ReactiveUserDetailsService reactiveUserDetailsService;
    private final JwtAuthenticationWebFilter jwtAuthenticationWebFilter;
    private final String allowedOrigins;

    @Autowired
    public SecurityConfig(ReactiveUserDetailsService reactiveUserDetailsService,
                          JwtAuthenticationWebFilter jwtAuthenticationWebFilter,
                          @Value("${cors.allowed.origins}") String allowedOrigins) {
        this.reactiveUserDetailsService = reactiveUserDetailsService;
        this.jwtAuthenticationWebFilter = jwtAuthenticationWebFilter;
        this.allowedOrigins = allowedOrigins;
        logger.info("SecurityConfig initialized with ReactiveUserDetailsService and JwtAuthenticationWebFilter.");
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager(PasswordEncoder passwordEncoder) {
        var authenticationManager = new UserDetailsRepositoryReactiveAuthenticationManager(this.reactiveUserDetailsService);
        authenticationManager.setPasswordEncoder(passwordEncoder);
        logger.info("ReactiveAuthenticationManager bean created.");
        return authenticationManager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.info("PasswordEncoder bean created.");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        logger.debug("Configuring CORS for origins: {}", allowedOrigins);
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        logger.info("CorsConfigurationSource bean created.");
        return source;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, ReactiveAuthenticationManager reactiveAuthenticationManager) {
        logger.info("Configuring SecurityWebFilterChain...");
        http
                .cors(corsSpec -> corsSpec.configurationSource(corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authenticationManager(reactiveAuthenticationManager)
                .exceptionHandling(exceptionHandlingSpec ->
                        exceptionHandlingSpec.authenticationEntryPoint((exchange, ex) -> {
                            logger.error("Unauthorized error: {}", ex.getMessage());
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                )
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/auth/**").permitAll() // Allow auth endpoints
                        .pathMatchers(HttpMethod.OPTIONS).permitAll() // Allow CORS preflight
                        // --- V V V Explicitly protect bookmark and feed endpoints V V V ---
                        .pathMatchers("/api/feed/**").authenticated()
                        .pathMatchers("/api/bookmarks/**").authenticated()
                        // --- ^ ^ ^ End explicit protection ^ ^ ^ ---
                        .anyExchange().authenticated() // Secure all others (can be kept as a fallback)
                )
                .addFilterAt(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        logger.info("SecurityWebFilterChain configured successfully.");
        return http.build();
    }
}
