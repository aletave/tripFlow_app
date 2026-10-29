package com.tripflow.itinerary_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentazione Swagger/OpenAPI del servizio, con autenticazione Bearer JWT.
 * Attiva solo se abilitata da configurazione (profilo {@code dev}).
 */
@Configuration
public class OpenApiConfig {

    /**
     * Titolo, versione e schema di sicurezza mostrati in Swagger UI.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()

                .info(new Info()
                        .title("Itinerary Service API")
                        .version("1.0.0")
                        .description("Documentazione delle api rest di itinerari e tappe di TripFlow"))

                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))

                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
