package com.example.identitymanagementservice.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            System.out.println("[JpaAuditConfig] Authentication: " + authentication);

            if (authentication == null || !authentication.isAuthenticated()) {
                System.out.println("[JpaAuditConfig] No authenticated user found, using SYSTEM");
                return Optional.of("SYSTEM");
            }

            Object principal = authentication.getPrincipal();
            System.out.println("[JpaAuditConfig] Principal class: " + (principal != null ? principal.getClass() : "null"));
            System.out.println("[JpaAuditConfig] Principal: " + principal);

            if (principal instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                String username = jwt.getClaimAsString("preferred_username"); // or use "sub" or "email"
                System.out.println("[JpaAuditConfig] Extracted username from JWT: " + username);
                return Optional.ofNullable(username);
            }

            // fallback to authentication name
            String fallbackUsername = authentication.getName();
            System.out.println("[JpaAuditConfig] Fallback username: " + fallbackUsername);
            return Optional.ofNullable(fallbackUsername);
        };
    }

    @Bean
    public org.springframework.data.auditing.DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
