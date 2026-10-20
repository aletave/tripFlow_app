package com.tripflow.user_auth_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Risposta del refresh token contenente i nuovi token JWT")
public record TokenRefreshResponse(
        @Schema(description = "Nuovo JWT Access Token emesso da Keycloak")
        String accessToken,

        @Schema(description = "Nuovo Refresh Token (se rotazione abilitata) o refresh token esistente")
        String refreshToken,

        @Schema(description = "Secondi di validità dell'access token", example = "300")
        Long expiresIn,

        @Schema(description = "Tipo di token", example = "Bearer")
        String tokenType
) {
}
