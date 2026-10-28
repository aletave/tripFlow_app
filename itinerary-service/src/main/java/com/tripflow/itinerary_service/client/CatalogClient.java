package com.tripflow.itinerary_service.client;

import com.tripflow.itinerary_service.exception.CatalogServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

// Client inter-servizio verso catalog-service: verifica che viaggi e attivita'
// referenziati da una tappa esistano davvero prima di salvarla.
// Il RestClient (con timeout espliciti) arriva da CatalogClientConfig.
@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(@Qualifier("catalogRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public boolean tripExists(UUID tripId) {
        return exists("/api/v1/trips/{id}", tripId, "viaggio");
    }

    public boolean activityExists(UUID activityId) {
        return exists("/api/v1/activities/{id}", activityId, "attivita'");
    }

    // true se il catalogo risponde 200, false se 404. Timeout, errori di rete e 5xx
    // diventano CatalogServiceUnavailableException (503).
    private boolean exists(String path, UUID id, String label) {
        try {
            restClient.get()
                    .uri(path, id)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException e) {
            throw new CatalogServiceUnavailableException(
                    "Impossibile verificare il " + label + " " + id + " su catalog-service", e);
        }
    }
}
