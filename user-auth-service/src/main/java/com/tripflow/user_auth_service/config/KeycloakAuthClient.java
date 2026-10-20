package com.tripflow.user_auth_service.config;

import com.tripflow.user_auth_service.dto.response.KeycloakTokenResponse;
import com.tripflow.user_auth_service.exception.InvalidCredentialsException;
import com.tripflow.user_auth_service.exception.KeycloakOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client HTTP sincrono verso l'endpoint OAuth2 / OpenID Connect token di Keycloak.
 * Utilizza Spring RestClient (Spring 6.1+) per scambiare credenziali con token JWT.
 */
@Component
@Slf4j
public class KeycloakAuthClient {

    private final RestClient restClient;
    private final String tokenUrl;
    private final String revokeUrl;
    private final String clientId;
    private final String clientSecret;

    public KeycloakAuthClient(
            @Value("${keycloak.server-url}") String serverUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.auth-client-id:tripflow-app}") String clientId,
            @Value("${keycloak.auth-client-secret:}") String clientSecret,
            RestClient.Builder restClientBuilder
    ) {
        this.restClient = restClientBuilder.build();
        this.tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", serverUrl, realm);
        this.revokeUrl = String.format("%s/realms/%s/protocol/openid-connect/revoke", serverUrl, realm);
        this.clientId = clientId;
        this.clientSecret = clientSecret != null ? clientSecret.trim() : "";
    }

    /**
     * Esegue il login tramite grant_type=password (Direct Access Grants) su Keycloak.
     */
    public KeycloakTokenResponse login(String username, String password) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        if (!clientSecret.isBlank()) {
            formData.add("client_secret", clientSecret);
        }
        formData.add("username", username);
        formData.add("password", password);

        try {
            return restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST || ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.warn("Tentativo di autenticazione fallito per username={}: status={}", username, ex.getStatusCode());
                throw new InvalidCredentialsException("Email o password non validi");
            }
            log.error("Errore HTTP durante il login su Keycloak per username={}: status={}", username, ex.getStatusCode(), ex);
            throw new KeycloakOperationException("Errore durante l'autenticazione su Keycloak: " + ex.getMessage(), ex);
        } catch (RestClientException ex) {
            log.error("Impossibile raggiungere Keycloak token endpoint per username={}", username, ex);
            throw new KeycloakOperationException("Servizio di autenticazione temporaneamente non disponibile", ex);
        }
    }

    /**
     * Rinnova l'access token tramite grant_type=refresh_token su Keycloak.
     */
    public KeycloakTokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", clientId);
        if (!clientSecret.isBlank()) {
            formData.add("client_secret", clientSecret);
        }
        formData.add("refresh_token", refreshToken);

        try {
            return restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST || ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.warn("Refresh token non valido o revocato");
                throw new InvalidCredentialsException("Refresh token non valido o scaduto");
            }
            log.error("Errore HTTP durante il refresh del token su Keycloak: status={}", ex.getStatusCode(), ex);
            throw new KeycloakOperationException("Errore durante il refresh del token: " + ex.getMessage(), ex);
        } catch (RestClientException ex) {
            log.error("Impossibile raggiungere Keycloak token endpoint per refresh token", ex);
            throw new KeycloakOperationException("Servizio di autenticazione temporaneamente non disponibile", ex);
        }
    }

    /**
     * Revoca un refresh token tramite l'endpoint di revoca RFC 7009 su Keycloak.
     * Se il token è già revocato o scaduto, RFC 7009 specifica che l'endpoint restituisce 200 OK.
     */
    public void logout(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        if (!clientSecret.isBlank()) {
            formData.add("client_secret", clientSecret);
        }
        formData.add("token", refreshToken);
        formData.add("token_type_hint", "refresh_token");

        try {
            restClient.post()
                    .uri(revokeUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Token revocato con successo su Keycloak");
        } catch (HttpClientErrorException ex) {
            log.warn("Errore HTTP durante la revoca del token su Keycloak: status={}", ex.getStatusCode());
        } catch (RestClientException ex) {
            log.error("Impossibile raggiungere Keycloak revoke endpoint per logout", ex);
            throw new KeycloakOperationException("Servizio di autenticazione temporaneamente non disponibile per logout", ex);
        }
    }
}
