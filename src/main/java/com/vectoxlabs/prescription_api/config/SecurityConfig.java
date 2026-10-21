package com.vectoxlabs.prescription_api.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
        // Disable CSRF because we are a stateless REST API, not a browser-based app
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth-> auth
                        // Only users with the "DOCTOR" or "ADMIN" role can create prescriptions
                        .requestMatchers(HttpMethod.POST, "/api/v1/records").hasAnyRole("DOCTOR", "ADMIN")
                        // Any other endpoints (if added later) require standard authentication
                        .anyRequest().authenticated()
                )
                // Enable HTTP Basic Authentication
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(){
        // Creating two in-memory users to demonstrate Role-Based Authorization
        UserDetails doctor = User.withDefaultPasswordEncoder()
                .username("doctor1")
                .password("password123")
                .roles("DOCTOR")
                .build();

        UserDetails receptionist = User.withDefaultPasswordEncoder()
                .username("staff1")
                .password("staffpass")
                .roles("STAFF") // Cannot create prescriptions
                .build();

        return new InMemoryUserDetailsManager(doctor, receptionist);
    }
}
