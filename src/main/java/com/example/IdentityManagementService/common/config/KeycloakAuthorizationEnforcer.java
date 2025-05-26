package com.example.IdentityManagementService.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;





import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAuthorizationEnforcer {

    @Value("${keycloak.auth-server-url}")
    private String keycloakBaseUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isAuthorized(String token, String resource, String scope) {
        String url = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakBaseUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Bearer " + token);

        String encodedPermission = URLEncoder.encode(resource + "#" + scope, StandardCharsets.UTF_8);
        String body = String.format(
                "grant_type=urn:ietf:params:oauth:grant-type:uma-ticket" +
                        "&audience=%s" +
                        "&permission=%s" +
                        "&response_mode=decision",
                clientId, encodedPermission);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            // Successful response means authorized
            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("UMA Authorization successful for {}#{}", resource, scope);
                return true;
            }

            // Check for RPT (Requesting Party Token) in response
            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                return true;
            }

            log.warn("UMA Authorization failed for {}#{}: {}", resource, scope, response.getBody());
            return false;

        } catch (HttpClientErrorException e) {
            log.error("UMA Authorization error: {}", e.getResponseBodyAsString());
            return false;
        }
    }
}