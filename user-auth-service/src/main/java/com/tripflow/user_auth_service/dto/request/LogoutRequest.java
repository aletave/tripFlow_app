package com.tripflow.user_auth_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Richiesta di logout con revoca del refresh token")
public record LogoutRequest(
        @Schema(description = "Refresh token rilasciato all'autenticazione da revocare", example = "eyJhbGciOi...")
        @NotBlank(message = "Il refresh token è obbligatorio per il logout")
        String refreshToken
) {
}
