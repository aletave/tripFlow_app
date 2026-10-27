package com.tripflow.itinerary_service.client;

import com.tripflow.itinerary_service.exception.UserAuthServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

// Client inter-servizio verso user-auth-service.
// Serve a verificare che un utente esista davvero prima di condividere un itinerario.
// Il RestClient arriva da UserAuthClientConfig, dove sono configurati i timeout
// espliciti di connect/read: un auth-service lento non puo' tenere bloccata
// indefinitamente la richiesta.
@Component
public class UserAuthClient {

    private final RestClient restClient;

    public UserAuthClient(@Qualifier("userAuthRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    // Ritorna true se lo user-auth-service riconosce l'utente (200),
    // false se non esiste (404). Timeout, errori di rete e 5xx vengono
    // propagati come UserAuthServiceUnavailableException (503).
    public boolean userExists(UUID keycloakId) {
        try {
            restClient.get()
                    .uri("/api/auth/users/by-keycloak/{keycloakId}", keycloakId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException e) {
            throw new UserAuthServiceUnavailableException(
                    "Impossibile verificare l'utente " + keycloakId + " su user-auth-service", e);
        }
    }
}
