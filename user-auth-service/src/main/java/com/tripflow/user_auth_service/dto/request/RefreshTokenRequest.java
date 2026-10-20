package com.tripflow.user_auth_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Richiesta di refresh del token JWT")
public record RefreshTokenRequest(
        @Schema(description = "Refresh token valido rilasciato da Keycloak")
        @NotBlank(message = "Il refresh token è obbligatorio")
        String refreshToken
) {
}
