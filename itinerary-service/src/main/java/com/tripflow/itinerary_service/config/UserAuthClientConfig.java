package com.tripflow.itinerary_service.config;

import com.tripflow.itinerary_service.client.BearerTokenForwardingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// RestClient dedicato a user-auth-service.
// I timeout di connect/read evitano che un rallentamento dell'auth-service
// saturi il thread pool di Tomcat e, indirettamente, il pool HikariCP.
@Configuration
public class UserAuthClientConfig {

    @Bean
    public RestClient userAuthRestClient(
            RestClient.Builder builder,
            BearerTokenForwardingInterceptor bearerTokenForwardingInterceptor,
            @Value("${tripflow.user-auth-service.base-url:http://localhost:8086}") String baseUrl,
            @Value("${tripflow.user-auth-service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${tripflow.user-auth-service.read-timeout-ms:2000}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        return builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(bearerTokenForwardingInterceptor)
                .build();
    }
}
