package com.tripflow.user_auth_service.config;

import com.tripflow.user_auth_service.exception.KeycloakOperationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

//Client dell'Admin API di Keycloak usato per registrazione, eliminazione e cambio password.
//Nota: istanza admin creata ad ogni chiamata, accettabile per il carico del progetto;
//in produzione andrebbe cachata/pool.
@Component
@Slf4j
public class KeycloakAdminClient {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realmName;

    @Value("${keycloak.admin-username}")
    private String adminUsername;

    @Value("${keycloak.admin-password}")
    private String adminPassword;

    //Crea l'utente su Keycloak, assegna il ruolo e restituisce il keycloak_id.
    //Se Keycloak risponde 409 (utente già presente ma orfano, assente dal DB locale)
    //recupera l'UUID esistente cercando per email.
    public String createUser(String email, String rawPassword,
                             String firstName, String lastName, String roleName) {
        Keycloak keycloak = getInstance();
        String keycloakId;

        try {
            UserRepresentation user = new UserRepresentation();
            user.setUsername(email);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(true);
            user.setEmailVerified(true);

            CredentialRepresentation credenziale = new CredentialRepresentation();
            credenziale.setType(CredentialRepresentation.PASSWORD);
            credenziale.setValue(rawPassword);
            credenziale.setTemporary(false);
            user.setCredentials(List.of(credenziale));

            try (Response response = keycloak.realm(realmName).users().create(user)) {
                if (response.getStatus() == 201) {
                    keycloakId = estraiIdDallaLocation(response);
                } else if (response.getStatus() == 409) {
                    log.warn("409 su Keycloak per l'email {}, recupero utente orfano", email);
                    keycloakId = trovaUtentePerEmail(keycloak, email);
                } else {
                    throw new KeycloakOperationException(
                            "Impossibile creare l'utente " + email + " su Keycloak (status " + response.getStatus() + ")");
                }
            }

            assegnaRuolo(keycloak, keycloakId, roleName);
            log.info("Utente creato su Keycloak: email={}, keycloak_id={}", email, keycloakId);
            return keycloakId;

        } catch (KeycloakOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Errore durante la creazione dell'utente {} su Keycloak", email, e);
            throw new KeycloakOperationException("Errore durante la registrazione su Keycloak", e);
        }
    }

    //Elimina l'utente da Keycloak.
    public void deleteUser(String keycloakId) {
        try {
            getInstance().realm(realmName).users().get(keycloakId).remove();
            log.info("Utente eliminato da Keycloak: keycloak_id={}", keycloakId);
        } catch (Exception e) {
            log.error("Errore durante l'eliminazione dell'utente {} su Keycloak", keycloakId, e);
            throw new KeycloakOperationException("Errore durante l'eliminazione dell'utente su Keycloak", e);
        }
    }

    //Aggiorna la password dell'utente su Keycloak.
    public void changePassword(String keycloakId, String newPassword) {
        try {
            CredentialRepresentation credenziale = new CredentialRepresentation();
            credenziale.setType(CredentialRepresentation.PASSWORD);
            credenziale.setValue(newPassword);
            credenziale.setTemporary(false);

            getInstance().realm(realmName).users().get(keycloakId).resetPassword(credenziale);
            log.info("Password aggiornata su Keycloak per keycloak_id={}", keycloakId);
        } catch (Exception e) {
            log.error("Errore durante l'aggiornamento della password per l'utente {} su Keycloak", keycloakId, e);
            throw new KeycloakOperationException("Errore durante l'aggiornamento della password su Keycloak", e);
        }
    }

    //Istanza admin: realm "master" + client "admin-cli", credenziali da properties.
    private Keycloak getInstance() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")
                .clientId("admin-cli")
                .grantType(OAuth2Constants.PASSWORD)
                .username(adminUsername)
                .password(adminPassword)
                .build();
    }

    //L'UUID del nuovo utente è nell'ultimo segmento della Location header della response 201.
    private String estraiIdDallaLocation(Response response) {
        String location = response.getLocation() != null ? response.getLocation().getPath() : "";
        if (location.isBlank()) {
            throw new KeycloakOperationException("Creazione utente riuscita ma Location header assente");
        }
        return location.substring(location.lastIndexOf('/') + 1);
    }

    //Recovery dell'utente orfano: ricerca esatta per email.
    private String trovaUtentePerEmail(Keycloak keycloak, String email) {
        List<UserRepresentation> trovati = keycloak.realm(realmName).users().searchByEmail(email, true);
        if (trovati == null || trovati.isEmpty()) {
            throw new KeycloakOperationException(
                    "Conflitto su Keycloak per " + email + " ma nessun utente trovato");
        }
        return trovati.get(0).getId();
    }

    //Assegna il ruolo realm (TRAVELER o ORGANIZER) all'utente.
    private void assegnaRuolo(Keycloak keycloak, String keycloakId, String roleName) {
        RoleRepresentation ruolo = keycloak.realm(realmName).roles().get(roleName).toRepresentation();
        keycloak.realm(realmName).users().get(keycloakId)
                .roles().realmLevel().add(List.of(ruolo));
    }
}