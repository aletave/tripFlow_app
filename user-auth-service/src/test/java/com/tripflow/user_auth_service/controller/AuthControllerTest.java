package com.tripflow.user_auth_service.controller;

import com.nimbusds.jose.jwk.RSAKey;
import com.tripflow.user_auth_service.config.AvatarStorageConfig;
import com.tripflow.user_auth_service.configSecurity.SecurityConfig;
import com.tripflow.user_auth_service.dto.response.LoginResponse;
import com.tripflow.user_auth_service.dto.response.PublicUserResponse;
import com.tripflow.user_auth_service.dto.response.RegisterResponse;
import com.tripflow.user_auth_service.dto.response.TokenRefreshResponse;
import com.tripflow.user_auth_service.dto.response.UserResponse;
import com.tripflow.user_auth_service.exception.EmailAlreadyExistsException;
import com.tripflow.user_auth_service.exception.InvalidCredentialsException;
import com.tripflow.user_auth_service.exception.KeycloakOperationException;
import com.tripflow.user_auth_service.exception.ResourceNotFoundException;
import com.tripflow.user_auth_service.model.Role;
import com.tripflow.user_auth_service.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthControllerTest.TestJwtConfig.class})
class AuthControllerTest {

    private static final UUID ID = UUID.fromString("70cd15d7-3a55-407c-9909-1f575e86cbfe");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private AvatarStorageConfig avatarStorage;

    private static final String REGISTER_BODY = """
            {
              "firstName": "Mario",
              "lastName": "Rossi",
              "email": "mario.rossi@example.com",
              "password": "password",
              "role": "TRAVELER",
              "dateOfBirth": "1990-01-01"
            }
            """;

    @Test
    void register_valido_201() throws Exception {
        when(authService.register(any())).thenReturn(new RegisterResponse(ID, "mario.rossi@example.com", true, true));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.email").value("mario.rossi@example.com"))
                .andExpect(jsonPath("$.requiresLogin").value(true));
    }

    @Test
    void corsPreflight_daGateway_rispondeConAccessControlAllowOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/register")
                        .header(HttpHeaders.ORIGIN, "http://localhost:8080")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8080"));
    }

    @Test
    void register_jsonMalformato_400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ questa non è una stringa JSON valida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo della richiesta non leggibile o JSON malformato"));
    }

    @Test
    void register_bodyInvalido_400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Mario",
                                  "lastName": "Rossi",
                                  "email": "email-non-valida",
                                  "password": "short",
                                  "role": "TRAVELER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void register_telefonoE164_201() throws Exception {
        when(authService.register(any())).thenReturn(new RegisterResponse(ID, "mario.rossi@example.com", true, true));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY.replace("\"dateOfBirth\": \"1990-01-01\"",
                                "\"dateOfBirth\": \"1990-01-01\", \"phoneNumber\": \"+393331234567\"")))
                .andExpect(status().isCreated());
    }

    @Test
    void register_telefonoSenzaPrefisso_400() throws Exception {
        for (String telefono : new String[]{"3331234567", "+39 333 123 4567", "+39", "+0391234567"}) {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REGISTER_BODY.replace("\"dateOfBirth\": \"1990-01-01\"",
                                    "\"dateOfBirth\": \"1990-01-01\", \"phoneNumber\": \"" + telefono + "\"")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("phoneNumber")));
        }
        verify(authService, org.mockito.Mockito.never()).register(any());
    }

    @Test
    void register_emailDuplicata_409() throws Exception {
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException("mario.rossi@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void register_keycloakGiù_502() throws Exception {
        when(authService.register(any()))
                .thenThrow(new KeycloakOperationException("Errore durante la registrazione su Keycloak"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isBadGateway());
    }

    @Test
    void login_successo_200() throws Exception {
        UserResponse user = new UserResponse(ID, "Mario", "Rossi", "mario.rossi@example.com",
                Role.TRAVELER, LocalDate.of(1990, 1, 1), "3331234567", null);
        LoginResponse response = new LoginResponse("mock-access-token", "mock-refresh-token", 300L, "Bearer", user);

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "mario.rossi@example.com",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.id").value(ID.toString()))
                .andExpect(jsonPath("$.user.email").value("mario.rossi@example.com"));
    }

    @Test
    void login_credenzialiInvalide_401() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException("Email o password non validi"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "mario.rossi@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email o password non validi"));
    }

    @Test
    void login_validazioneFallita_400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "non-un-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void refresh_successo_200() throws Exception {
        TokenRefreshResponse response = new TokenRefreshResponse("new-access-token", "new-refresh-token", 300L, "Bearer");
        when(authService.refresh(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "valid-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(300));
    }

    @Test
    void refresh_tokenInvalido_401() throws Exception {
        when(authService.refresh(any())).thenThrow(new InvalidCredentialsException("Refresh token non valido o scaduto"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "expired-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token non valido o scaduto"));
    }

    @Test
    void logout_successo_204() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "valid-refresh-token"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(authService).logout(any());
    }

    @Test
    void logout_senzaRefreshToken_400() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("refresh token")));
    }

    @Test
    void routeInesistente_404() throws Exception {
        mockMvc.perform(get("/api/auth/inesistente")
                        .with(jwt().jwt(j -> j.subject("kc-123"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Risorsa non trovata: api/auth/inesistente"));
    }

    @Test
    void metodoNonSupportato_405() throws Exception {
        mockMvc.perform(post("/api/auth/me")
                        .with(jwt().jwt(j -> j.subject("kc-123"))))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Metodo HTTP non supportato: POST"));
    }

    @Test
    void getMe_senzaToken_401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_conTokenValido_200() throws Exception {
        when(authService.getMe("kc-123")).thenReturn(new UserResponse(
                ID, "Mario", "Rossi", "mario.rossi@example.com",
                Role.TRAVELER, LocalDate.of(1990, 1, 1), "3331234567", null));

        mockMvc.perform(get("/api/auth/me")
                        .with(jwt().jwt(j -> j.subject("kc-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.role").value("TRAVELER"));

        verify(authService).getMe("kc-123");
    }

    @Test
    void getMe_erroreInterno_500ConMessaggioGenerico() throws Exception {
        when(authService.getMe("kc-123")).thenThrow(new RuntimeException("dettaglio interno non esposto"));

        mockMvc.perform(get("/api/auth/me")
                        .with(jwt().jwt(j -> j.subject("kc-123"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Errore interno del server"));
    }

    @Test
    void updateMe_conTokenValido_200() throws Exception {
        when(authService.updateProfile(any(), any())).thenReturn(new UserResponse(
                ID, "Mario", "Rossi", "mario.rossi@example.com",
                Role.TRAVELER, LocalDate.of(1990, 1, 1), "3331234567", null));

        mockMvc.perform(put("/api/auth/me")
                        .with(jwt().jwt(j -> j.subject("kc-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Mario", "lastName": "Rossi"}
                                """))
                .andExpect(status().isOk());

        verify(authService).updateProfile(eq("kc-123"), any());
    }

    @Test
    void updateMe_telefonoNonInternazionale_400() throws Exception {
        mockMvc.perform(put("/api/auth/me")
                        .with(jwt().jwt(j -> j.subject("kc-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Mario", "lastName": "Rossi", "phoneNumber": "333-1234567"}
                                """))
                .andExpect(status().isBadRequest());

        verify(authService, org.mockito.Mockito.never()).updateProfile(any(), any());
    }

    @Test
    void changePassword_conTokenValido_204() throws Exception {
        mockMvc.perform(put("/api/auth/me/password")
                        .with(jwt().jwt(j -> j.subject("kc-123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "password1", "newPassword": "password2"}
                                """))
                .andExpect(status().isNoContent());

        verify(authService).changePassword(eq("kc-123"), any());
    }

    @Test
    void deleteMe_conTokenValido_204() throws Exception {
        mockMvc.perform(delete("/api/auth/me")
                        .with(jwt().jwt(j -> j.subject("kc-123"))))
                .andExpect(status().isNoContent());

        verify(authService).deleteMe("kc-123");
    }

    @Test
    void getPublicUserByKeycloakId_esistente_200() throws Exception {
        when(authService.getPublicUserByKeycloakId("kc-123")).thenReturn(new PublicUserResponse(
                ID, "Mario", "Rossi", Role.TRAVELER, null));

        mockMvc.perform(get("/api/auth/users/by-keycloak/kc-123")
                        .with(jwt().jwt(j -> j.subject("kc-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.firstName").value("Mario"))
                .andExpect(jsonPath("$.role").value("TRAVELER"));

        verify(authService).getPublicUserByKeycloakId("kc-123");
    }

    @Test
    void getPublicUserByKeycloakId_inesistente_404() throws Exception {
        when(authService.getPublicUserByKeycloakId("kc-inesistente"))
                .thenThrow(new ResourceNotFoundException("Utente non trovato con keycloak_id: kc-inesistente"));

        mockMvc.perform(get("/api/auth/users/by-keycloak/kc-inesistente")
                        .with(jwt().jwt(j -> j.subject("kc-123"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadAvatar_conFileValido_200() throws Exception {
        when(authService.getMe("kc-123")).thenReturn(new UserResponse(
                ID, "Mario", "Rossi", "mario.rossi@example.com",
                Role.TRAVELER, LocalDate.of(1990, 1, 1), null, null));
        when(avatarStorage.salva(any())).thenReturn("avatar-123.jpg");

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", "dummy-image-content".getBytes());

        mockMvc.perform(multipart("/api/auth/me/avatar")
                        .file(file)
                        .with(jwt().jwt(j -> j.subject("kc-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").isNotEmpty());

        verify(authService).updateProfileImage(eq("kc-123"), any());
    }

    @Test
    void uploadAvatar_senzaAutenticazione_401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", "dummy-image-content".getBytes());

        mockMvc.perform(multipart("/api/auth/me/avatar")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    //JwtDecoder locale con chiave RSA di test: la security chain di SecurityConfig
    //non deve contattare Keycloak/JWKS durante i test.
    @TestConfiguration
    static class TestJwtConfig {

        @Bean
        JwtDecoder jwtDecoder() throws Exception {
            KeyPairGenerator generatore = KeyPairGenerator.getInstance("RSA");
            generatore.initialize(2048);
            KeyPair coppia = generatore.generateKeyPair();
            RSAKey chiave = new RSAKey.Builder((RSAPublicKey) coppia.getPublic()).build();
            return NimbusJwtDecoder.withPublicKey(chiave.toRSAPublicKey()).build();
        }
    }
}