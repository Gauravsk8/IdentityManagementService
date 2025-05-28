package com.example.identitymanagementservice.config;


import com.example.identitymanagementservice.common.security.CustomAccessDeniedHandler;
import com.example.identitymanagementservice.common.security.CustomEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static com.example.identitymanagementservice.utils.SecurityUtils.csrfConfig;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomEntryPoint customEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(CustomEntryPoint customEntryPoint,
                          CustomAccessDeniedHandler customAccessDeniedHandler) {
        this.customEntryPoint = customEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Value("${keycloak.enabled:true}")
    private boolean keycloakEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())

                // Re-enable CSRF with cookie token repository via common module
                .csrf(csrfConfig())

                // Allow unauthenticated access to Swagger
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Handle exceptions
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                // Enable OAuth2 Resource Server support (JWT)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(customEntryPoint)
                        .jwt(Customizer.withDefaults())
                );

        return http.build();
    }
}
