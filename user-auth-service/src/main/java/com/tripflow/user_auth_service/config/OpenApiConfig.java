package com.tripflow.user_auth_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()

                .info(new Info()
                        .title("User Auth Service API")
                        .version("1.0.0")
                        .description("Documentazione delle api rest di registrazione e profilo utente di TripFlow"))

                //"Try it out" passa dal gateway; il secondo server serve quando si apre la
                //Swagger direttamente sul servizio avviato in locale (es. da IntelliJ)
                .addServersItem(new Server().url("/user-auth-service").description("Tramite gateway"))
                .addServersItem(new Server().url("/").description("Servizio diretto (sviluppo locale)"))

                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))

                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}