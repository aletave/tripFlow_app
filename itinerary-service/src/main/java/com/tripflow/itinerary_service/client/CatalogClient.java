package com.tripflow.itinerary_service.client;

import com.tripflow.itinerary_service.exception.CatalogServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * Client verso catalog-service: verifica che viaggi e attivita' citati da una tappa
 * esistano davvero prima di salvarla.
 *
 * <p>Il {@link RestClient} (con timeout espliciti) arriva da
 * {@link com.tripflow.itinerary_service.config.CatalogClientConfig}.
 */
@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(@Qualifier("catalogRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * @return {@code true} se il viaggio esiste, {@code false} se il catalogo risponde 404
     * @throws CatalogServiceUnavailableException se il catalogo non risponde o da' un errore
     */
    public boolean tripExists(UUID tripId) {
        return exists("/api/v1/trips/{id}", tripId, "viaggio");
    }

    /**
     * @return {@code true} se l'attivita' esiste, {@code false} se il catalogo risponde 404
     * @throws CatalogServiceUnavailableException se il catalogo non risponde o da' un errore
     */
    public boolean activityExists(UUID activityId) {
        return exists("/api/v1/activities/{id}", activityId, "attivita'");
    }

    // 200 -> true, 404 -> false. Timeout, errori di rete e 5xx diventano
    // CatalogServiceUnavailableException (503).
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
