package com.tripflow.itinerary_service.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

/**
 * Inoltra alle chiamate verso altri servizi l'header {@code Authorization} della richiesta
 * in ingresso.
 *
 * <p>catalog-service e user-auth-service richiedono un JWT valido: la chiamata
 * inter-servizio viaggia quindi con le credenziali dell'utente che l'ha originata.
 */
@Component
public class BearerTokenForwardingInterceptor implements ClientHttpRequestInterceptor {

    /**
     * Copia il token della richiesta corrente, se presente. Fuori da una richiesta HTTP
     * (es. listener RabbitMQ) la chiamata parte senza token.
     */
    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            String authorization = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && !authorization.isBlank()) {
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
        return execution.execute(request, body);
    }
}
