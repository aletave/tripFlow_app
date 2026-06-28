package com.tripflow.review.configSecurity;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propaga l'header Authorization (Bearer) della richiesta entrante alle chiamate Feign
 * (BookingClient, CatalogClient), così che anche quei servizi validino lo stesso JWT.
 * Senza una richiesta HTTP in corso (es. listener eventi) non fa nulla.
 */
@Configuration
public class FeignAuthInterceptor {

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                String auth = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (auth != null && !auth.isBlank()) {
                    template.header(HttpHeaders.AUTHORIZATION, auth);
                }
            }
        };
    }
}