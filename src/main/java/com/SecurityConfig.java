package com;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // public endpoints
                        .requestMatchers("/", "/home", "/login", "/register/**", "/auth/**", "/css/**",
                                "/event", "/add-event", "/addEvent", "/test-db").permitAll()

                        // role-based access
                        .requestMatchers("/member/**").hasAuthority("MEMBER")
                        .requestMatchers("/club/**").hasAuthority("CLUB_HEAD")   // ⚠️ fix path if it's /club/** not /clubhead/**
                        .requestMatchers("/faculty/**").hasAuthority("FACULTY")

                        // everything else requires authentication
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true)   // redirect after login
                        .permitAll()
                )
                .logout(logout -> logout.permitAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
