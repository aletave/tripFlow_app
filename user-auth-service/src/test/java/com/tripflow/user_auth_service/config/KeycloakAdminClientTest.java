package com.tripflow.user_auth_service.config;

import com.tripflow.user_auth_service.exception.EmailAlreadyExistsException;
import com.tripflow.user_auth_service.exception.KeycloakOperationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeycloakAdminClientTest {

    private static final String EMAIL = "mario@example.com";

    private Keycloak keycloak;
    private KeycloakAdminClient client;

    @BeforeEach
    void setUp() {
        keycloak = mock(Keycloak.class, RETURNS_DEEP_STUBS);
        client = spy(new KeycloakAdminClient());
        ReflectionTestUtils.setField(client, "serverUrl", "http://localhost:9090");
        ReflectionTestUtils.setField(client, "realmName", "tripflow");
        ReflectionTestUtils.setField(client, "adminUsername", "admin");
        ReflectionTestUtils.setField(client, "adminPassword", "admin");
        doReturn(keycloak).when(client).getInstance();
    }

    private Response responseConStatus(int status) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        when(keycloak.realm("tripflow").users().create(any())).thenReturn(response);
        return response;
    }

    @Test
    void createUser_201_ritornaIdEAssegnaRuolo() {
        Response response = responseConStatus(201);
        when(response.getLocation()).thenReturn(
                URI.create("http://localhost:9090/admin/realms/tripflow/users/kc-123"));

        String id = client.createUser(EMAIL, "password", "Mario", "Rossi", "TRAVELER", kcId -> true);

        assertEquals("kc-123", id);
        verify(keycloak.realm("tripflow").users().get("kc-123").roles().realmLevel()).add(anyList());
        verify(keycloak.realm("tripflow").users().get("kc-123"), never()).remove();
    }

    @Test
    void createUser_locationAssente_compensaCancellandoPerEmail() {
        responseConStatus(201);
        UserRepresentation orfano = new UserRepresentation();
        orfano.setId("kc-123");
        when(keycloak.realm("tripflow").users().searchByEmail(eq(EMAIL), eq(true)))
                .thenReturn(List.of(orfano));

        assertThrows(KeycloakOperationException.class,
                () -> client.createUser(EMAIL, "password", "Mario", "Rossi", "TRAVELER", kcId -> true));

        verify(keycloak.realm("tripflow").users().get("kc-123")).remove();
    }

    @Test
    void createUser_assegnazioneRuoloFallita_compensaCancellando() {
        Response response = responseConStatus(201);
        when(response.getLocation()).thenReturn(
                URI.create("http://localhost:9090/admin/realms/tripflow/users/kc-123"));
        when(keycloak.realm("tripflow").roles().get("TRAVELER").toRepresentation())
                .thenThrow(new RuntimeException("ruolo inesistente"));

        assertThrows(KeycloakOperationException.class,
                () -> client.createUser(EMAIL, "password", "Mario", "Rossi", "TRAVELER", kcId -> true));

        verify(keycloak.realm("tripflow").users().get("kc-123")).remove();
    }

    @Test
    void createUser_409_recuperaOrfanoEApplicaLaPasswordScelta() {
        responseConStatus(409);
        UserRepresentation orfano = new UserRepresentation();
        orfano.setId("kc-999");
        when(keycloak.realm("tripflow").users().searchByEmail(eq(EMAIL), eq(true)))
                .thenReturn(List.of(orfano));

        String id = client.createUser(EMAIL, "password", "Mario", "Rossi", "TRAVELER", kcId -> true);

        assertEquals("kc-999", id);
        verify(keycloak.realm("tripflow").users().get("kc-999")).resetPassword(
                any(CredentialRepresentation.class));
        verify(keycloak.realm("tripflow").users().get("kc-999").roles().realmLevel()).add(anyList());
    }

    @Test
    void createUser_409_accountGiaNelDb_conflittoSenzaToccareNulla() {
        responseConStatus(409);
        UserRepresentation accountEsistente = new UserRepresentation();
        accountEsistente.setId("kc-999");
        when(keycloak.realm("tripflow").users().searchByEmail(eq(EMAIL), eq(true)))
                .thenReturn(List.of(accountEsistente));

        assertThrows(EmailAlreadyExistsException.class,
                () -> client.createUser(EMAIL, "password", "Mario", "Rossi", "TRAVELER", kcId -> false));

        verify(keycloak.realm("tripflow").users().get("kc-999"), never()).resetPassword(any());
        verify(keycloak.realm("tripflow").users().get("kc-999"), never()).remove();
    }

    @Test
    void createUser_409_senzaOrfanoTrovato_lanciaErroreSenzaCancellareNulla() {
        responseConStatus(409);
        when(keycloak.realm("tripflow").users().searchByEmail(eq(EMAIL), eq(true)))
                .thenReturn(List.of());

        assertThrows(KeycloakOperationException.class,
                () -> client.createUser(EMAIL, "password", "Mario", "Rossi", "TRAVELER", kcId -> true));

        verify(keycloak.realm("tripflow").users().get("kc-999"), never()).remove();
    }

    @Test
    void createUser_409_adottatoConRuoloFallito_compensaCancellandoLOrfano() {
        responseConStatus(409);
        UserRepresentation orfano = new UserRepresentation();
        orfano.setId("kc-999");
        when(keycloak.realm("tripflow").users().searchByEmail(eq(EMAIL), eq(true)))
                .thenReturn(List.of(orfano));
        when(keycloak.realm("tripflow").roles().get("TRAVELER").toRepresentation())
                .thenThrow(new RuntimeException("ruolo inesistente"));

        assertThrows(KeycloakOperationException.class,
                () -> client.createUser(EMAIL, "password", "Mario", "Rossi", "TRAVELER", kcId -> true));

        //anche un orfano adottato va ripulito se la registrazione fallisce dopo l'adozione
        verify(keycloak.realm("tripflow").users().get("kc-999")).remove();
    }

    @Test
    void getInstance_ritornaSempreLaStessaIstanza() {
        KeycloakAdminClient clientFresco = new KeycloakAdminClient();
        ReflectionTestUtils.setField(clientFresco, "serverUrl", "http://localhost:9090");
        ReflectionTestUtils.setField(clientFresco, "realmName", "tripflow");
        ReflectionTestUtils.setField(clientFresco, "adminUsername", "admin");
        ReflectionTestUtils.setField(clientFresco, "adminPassword", "admin");

        //la build di KeycloakBuilder non contatta il server: si verifica solo il singleton
        assertSame(clientFresco.getInstance(), clientFresco.getInstance());
    }
}