package com.tripflow.user_auth_service.config;

import com.tripflow.user_auth_service.dto.response.KeycloakTokenResponse;
import com.tripflow.user_auth_service.exception.EmailNotVerifiedException;
import com.tripflow.user_auth_service.exception.InvalidCredentialsException;
import com.tripflow.user_auth_service.exception.KeycloakOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class KeycloakAuthClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private KeycloakAuthClient authClient;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        authClient = new KeycloakAuthClient(
                "http://localhost:9090",
                "tripflow",
                "tripflow-app",
                "",
                restClientBuilder
        );
    }

    @Test
    void login_successo_restituisceTokenResponse() {
        mockServer.expect(requestTo("http://localhost:9090/realms/tripflow/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess("""
                        {
                          "access_token": "mock-access-token",
                          "expires_in": 300,
                          "refresh_expires_in": 1800,
                          "refresh_token": "mock-refresh-token",
                          "token_type": "Bearer",
                          "scope": "openid email profile"
                        }
                        """, MediaType.APPLICATION_JSON));

        KeycloakTokenResponse response = authClient.login("mario@example.com", "password123");

        assertNotNull(response);
        assertEquals("mock-access-token", response.accessToken());
        assertEquals("mock-refresh-token", response.refreshToken());
        assertEquals(300L, response.expiresIn());
        assertEquals("Bearer", response.tokenType());
        mockServer.verify();
    }

    @Test
    void login_credenzialiErrate_400_lanciaInvalidCredentialsException() {
        mockServer.expect(requestTo("http://localhost:9090/realms/tripflow/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest().body("""
                        {
                          "error": "invalid_grant",
                          "error_description": "Invalid user credentials"
                        }
                        """).contentType(MediaType.APPLICATION_JSON));

        assertThrows(InvalidCredentialsException.class, () -> authClient.login("mario@example.com", "wrong"));
        mockServer.verify();
    }

    @Test
    void login_verificaEmailPendente_lanciaEmailNotVerifiedException() {
        mockServer.expect(requestTo("http://localhost:9090/realms/tripflow/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest().body("""
                        {
                          "error": "invalid_grant",
                          "error_description": "Account is not fully set up"
                        }
                        """).contentType(MediaType.APPLICATION_JSON));

        assertThrows(EmailNotVerifiedException.class, () -> authClient.login("mario@example.com", "password123"));
        mockServer.verify();
    }

    @Test
    void login_credenzialiErrate_401_lanciaInvalidCredentialsException() {
        mockServer.expect(requestTo("http://localhost:9090/realms/tripflow/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withUnauthorizedRequest());

        assertThrows(InvalidCredentialsException.class, () -> authClient.login("mario@example.com", "wrong"));
        mockServer.verify();
    }

    @Test
    void login_erroreServer_500_lanciaKeycloakOperationException() {
        mockServer.expect(requestTo("http://localhost:9090/realms/tripflow/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThrows(KeycloakOperationException.class, () -> authClient.login("mario@example.com", "password123"));
        mockServer.verify();
    }

    @Test
    void refreshToken_successo_restituisceNuovoToken() {
        mockServer.expect(requestTo("http://localhost:9090/realms/tripflow/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "new-access-token",
                          "expires_in": 300,
                          "refresh_token": "new-refresh-token",
                          "token_type": "Bearer"
                        }
                        """, MediaType.APPLICATION_JSON));

        KeycloakTokenResponse response = authClient.refreshToken("valid-refresh-token");

        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
        mockServer.verify();
    }

    @Test
    void refreshToken_scaduto_400_lanciaInvalidCredentialsException() {
        mockServer.expect(requestTo("http://localhost:9090/realms/tripflow/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest());

        assertThrows(InvalidCredentialsException.class, () -> authClient.refreshToken("expired"));
        mockServer.verify();
    }

    @Test
    void logout_successo_revocaToken() {
        mockServer.expect(requestTo("http://localhost:9090/realms/tripflow/protocol/openid-connect/revoke"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess());

        assertDoesNotThrow(() -> authClient.logout("refresh-token-da-revocare"));
        mockServer.verify();
    }

    @Test
    void logout_erroreServer_500_lanciaKeycloakOperationException() {
        mockServer.expect(requestTo("http://localhost:9090/realms/tripflow/protocol/openid-connect/revoke"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThrows(KeycloakOperationException.class, () -> authClient.logout("refresh-token-da-revocare"));
        mockServer.verify();
    }
}
