package com.tripflow.itinerary_service.config;

import com.tripflow.itinerary_service.client.BearerTokenForwardingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configura il {@link RestClient} usato per chiamare catalog-service.
 *
 * <p>URL e timeout si impostano da {@code application.properties}
 * ({@code tripflow.catalog-service.*}): un catalogo lento non deve bloccare i thread
 * di questo servizio.
 */
@Configuration
public class CatalogClientConfig {

    /**
     * RestClient con URL base, timeout e inoltro del token dell'utente.
     */
    @Bean
    public RestClient catalogRestClient(
            RestClient.Builder builder,
            BearerTokenForwardingInterceptor bearerTokenForwardingInterceptor,
            @Value("${tripflow.catalog-service.base-url:http://localhost:8084}") String baseUrl,
            @Value("${tripflow.catalog-service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${tripflow.catalog-service.read-timeout-ms:2000}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        // builder.clone(): il RestClient.Builder di Spring e' condiviso, non va modificato in place.
        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(bearerTokenForwardingInterceptor)
                .build();
    }
}
