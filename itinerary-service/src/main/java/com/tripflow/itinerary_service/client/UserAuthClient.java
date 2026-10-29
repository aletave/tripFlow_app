package com.tripflow.itinerary_service.client;

import com.tripflow.itinerary_service.exception.UserAuthServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * Client verso user-auth-service: verifica che un utente esista prima di condividere
 * un itinerario con lui.
 *
 * <p>Il {@link RestClient} arriva da
 * {@link com.tripflow.itinerary_service.config.UserAuthClientConfig}, con timeout espliciti:
 * un auth-service lento non puo' bloccare la richiesta all'infinito.
 */
@Component
public class UserAuthClient {

    private final RestClient restClient;

    public UserAuthClient(@Qualifier("userAuthRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * @param keycloakId id Keycloak dell'utente (il {@code sub} del suo token)
     * @return {@code true} se l'utente esiste, {@code false} se user-auth-service risponde 404
     * @throws UserAuthServiceUnavailableException timeout, errore di rete o 5xx (503)
     */
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
