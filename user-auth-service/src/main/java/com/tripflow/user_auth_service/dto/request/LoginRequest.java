package com.tripflow.user_auth_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Richiesta di login per ottenere token JWT e profilo utente")
public record LoginRequest(
        @Schema(description = "Indirizzo email dell'utente", example = "mario.rossi@example.com")
        @NotBlank(message = "L'email è obbligatoria")
        @Email(message = "Formato email non valido")
        String email,

        @Schema(description = "Password dell'account", example = "Password123!")
        @NotBlank(message = "La password è obbligatoria")
        String password
) {
}
