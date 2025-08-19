package com.feedfusion2.config;

import com.feedfusion2.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Configuration
public class AppConfig {

    // Inject the blocking service needed by the reactive adapter
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    /**
     * Defines the ReactiveUserDetailsService bean.
     * This adapts the existing blocking UserDetailsServiceImpl for reactive use.
     * Moved here from SecurityConfig to break the circular dependency.
     * @return ReactiveUserDetailsService bean
     */
    @Bean
    public ReactiveUserDetailsService reactiveUserDetailsService() {
        System.out.println("Creating ReactiveUserDetailsService bean..."); // Add log
        // Adapt the blocking service to the reactive interface
        return username -> Mono.fromCallable(() -> {
                    System.out.println("ReactiveUserDetailsService: Loading user " + username); // Add log
                    return userDetailsService.loadUserByUsername(username);
                })
                .subscribeOn(Schedulers.boundedElastic()); // Schedule the blocking call
    }

    // You can define other application-wide beans here if needed
}
