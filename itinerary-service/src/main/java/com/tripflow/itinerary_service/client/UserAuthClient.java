package com.tripflow.itinerary_service.client;

import com.tripflow.itinerary_service.exception.UserAuthServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

// Client inter-servizio verso user-auth-service.
// Serve a verificare che un utente esista davvero prima di condividere un itinerario.
// Usa RestClient (bloccante, nessuna dipendenza aggiuntiva) e propaga in avanti
// l'header Authorization della richiesta in ingresso, dato che l'endpoint di
// lookup su user-auth-service richiede un JWT valido.
@Component
public class UserAuthClient {

    private final RestClient restClient;

    public UserAuthClient(
            RestClient.Builder restClientBuilder,
            @Value("${tripflow.user-auth-service.base-url:http://localhost:8086}") String baseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestInterceptor(bearerTokenForwardingInterceptor())
                .build();
    }

    // Ritorna true se lo user-auth-service riconosce l'utente (200),
    // false se non esiste (404). Qualsiasi altro errore viene propagato
    // come UserAuthServiceUnavailableException (503).
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

    private ClientHttpRequestInterceptor bearerTokenForwardingInterceptor() {
        return (request, body, execution) -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                String authorization = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (authorization != null && !authorization.isBlank()) {
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorization);
                }
            }
            return execution.execute(request, body);
        };
    }
}
