package com.tripflow.user_auth_service.config;

import com.tripflow.user_auth_service.exception.EmailAlreadyExistsException;
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
import java.util.function.Predicate;

//Client dell'Admin API di Keycloak usato per registrazione, eliminazione e cambio password.
//L'istanza admin è unica e riusata da tutte le chiamate: il client è thread-safe
//e crearne una nuova ad ogni operazione accumulava connessioni mai chiuse.
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
    //Se Keycloak risponde 409 (utente già presente, magari orfano) recupera l'UUID per email,
    //prima di adottarlo chiede a chi chiama (orfanoAdottabile) se l'account non esiste già nel DB:
    //in quel caso è un conflitto vero e non lo si tocca.
    public String createUser(String email, String rawPassword,
                             String firstName, String lastName, String roleName,
                             Predicate<String> orfanoAdottabile) {
        Keycloak keycloak = getInstance();
        String keycloakId = null;
        boolean utenteCreato = false;
        boolean adottato = false;

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
                    utenteCreato = true;
                    keycloakId = estraiIdDallaLocation(response);
                } else if (response.getStatus() == 409) {
                    log.warn("409 su Keycloak per l'email {}, recupero utente orfano", email);
                    keycloakId = trovaUtentePerEmail(keycloak, email);
                    if (!orfanoAdottabile.test(keycloakId)) {
                        //l'account esiste già nel DB locale: è un conflitto reale, non un orfano
                        throw new EmailAlreadyExistsException(email);
                    }
                    //era orfano: ripristino la password scelta, altrimenti il login fallirebbe
                    adottato = true;
                    resetPassword(keycloak, keycloakId, rawPassword);
                } else {
                    throw new KeycloakOperationException(
                            "Impossibile creare l'utente " + email + " su Keycloak (status " + response.getStatus() + ")");
                }
            }

            assegnaRuolo(keycloak, keycloakId, roleName);
            log.info("Utente creato su Keycloak: email={}, keycloak_id={}", email, keycloakId);
            return keycloakId;

        } catch (Exception e) {
            //se qualcosa fallisce dopo la creazione (o l'adozione), si ripulisce l'account
            compensaUtenteAppenaCreato(keycloak, email, keycloakId, utenteCreato || adottato);
            if (e instanceof KeycloakOperationException koe) {
                throw koe;
            }
            if (e instanceof EmailAlreadyExistsException eae) {
                throw eae;
            }
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
            resetPassword(getInstance(), keycloakId, newPassword);
            log.info("Password aggiornata su Keycloak per keycloak_id={}", keycloakId);
        } catch (Exception e) {
            log.error("Errore durante l'aggiornamento della password per l'utente {} su Keycloak", keycloakId, e);
            throw new KeycloakOperationException("Errore durante l'aggiornamento della password su Keycloak", e);
        }
    }

    //Istanza admin: realm "master" + client "admin-cli", credenziali da properties.
    //Creata una sola volta e riusata (lazy con double-checked): il client è thread-safe.
    //Visibilità package-private per poterla mockare nei test.
    private volatile Keycloak istanza;

    Keycloak getInstance() {
        if (istanza == null) {
            synchronized (this) {
                if (istanza == null) {
                    istanza = KeycloakBuilder.builder()
                            .serverUrl(serverUrl)
                            .realm("master")
                            .clientId("admin-cli")
                            .grantType(OAuth2Constants.PASSWORD)
                            .username(adminUsername)
                            .password(adminPassword)
                            .build();
                }
            }
        }
        return istanza;
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

    //Imposta la nuova password sull'utente Keycloak.
    private void resetPassword(Keycloak keycloak, String keycloakId, String newPassword) {
        CredentialRepresentation credenziale = new CredentialRepresentation();
        credenziale.setType(CredentialRepresentation.PASSWORD);
        credenziale.setValue(newPassword);
        credenziale.setTemporary(false);
        keycloak.realm(realmName).users().get(keycloakId).resetPassword(credenziale);
    }

    //Se l'utente era stato appena creato (o adottato) ma dopo qualcosa è fallito
    //(es. ruolo inesistente, Location assente), lo cancelliamo per non lasciare orfani su Keycloak.
    private void compensaUtenteAppenaCreato(Keycloak keycloak, String email, String keycloakId, boolean daCompensare) {
        if (!daCompensare) {
            return;
        }
        String idDaCancellare = keycloakId;
        if (idDaCancellare == null) {
            try {
                idDaCancellare = trovaUtentePerEmail(keycloak, email);
            } catch (Exception e) {
                log.error("Impossibile risalire all'utente appena creato per la compensazione", e);
            }
        }
        if (idDaCancellare != null) {
            cancellaDopoErrore(keycloak, idDaCancellare);
        }
    }

    private void cancellaDopoErrore(Keycloak keycloak, String keycloakId) {
        try {
            keycloak.realm(realmName).users().get(keycloakId).remove();
            log.info("Compensazione: utente {} rimosso da Keycloak", keycloakId);
        } catch (Exception e) {
            log.error("Compensazione fallita: utente orfano su Keycloak {}", keycloakId, e);
        }
    }

    //Assegna il ruolo realm (TRAVELER o ORGANIZER) all'utente.
    private void assegnaRuolo(Keycloak keycloak, String keycloakId, String roleName) {
        RoleRepresentation ruolo = keycloak.realm(realmName).roles().get(roleName).toRepresentation();
        keycloak.realm(realmName).users().get(keycloakId)
                .roles().realmLevel().add(List.of(ruolo));
    }
}