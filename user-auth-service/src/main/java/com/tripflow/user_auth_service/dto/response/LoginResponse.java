package com.tripflow.user_auth_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Risposta del login contenente i token di autenticazione e il profilo utente completo")
public record LoginResponse(
        @Schema(description = "JWT Access Token emesso da Keycloak")
        String accessToken,

        @Schema(description = "Refresh Token per rinnovare la sessione")
        String refreshToken,

        @Schema(description = "Secondi di validità dell'access token", example = "300")
        Long expiresIn,

        @Schema(description = "Tipo di token", example = "Bearer")
        String tokenType,

        @Schema(description = "Profilo dell'utente autenticato")
        UserResponse user
) {
}
