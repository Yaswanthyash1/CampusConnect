package com.collegeclubs.event_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Allow REST API endpoints for microservice communication
                .requestMatchers("/events/**").permitAll()
                .requestMatchers("/api/events/**").permitAll()
                .requestMatchers("/api/events**").permitAll()
                .requestMatchers("/event-details/**").permitAll()
                // Allow any other requests
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
