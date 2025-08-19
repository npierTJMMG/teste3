package br.jus.tjm.ism.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Service
public class KeycloakTokenService {

    @Value("${keycloak.auth-server-url}")
    private String tokenUrl;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private String accessToken;
    private Instant expiresAt;

    public synchronized String getAccessToken() {
        if (accessToken == null || expiresAt == null || Instant.now().isAfter(expiresAt.minusSeconds(120))) {
            fetchToken();
        }
        return accessToken;
    }

    private void fetchToken() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "grant_type=client_credentials" +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret;

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Não foi possível obter o token do Keycloak");
        }

        Map<String, Object> tokenResponse = response.getBody();

        this.accessToken = (String) tokenResponse.get("access_token");
        Integer expiresIn = (Integer) tokenResponse.get("expires_in");
        this.expiresAt = Instant.now().plusSeconds(expiresIn);
    }
}
