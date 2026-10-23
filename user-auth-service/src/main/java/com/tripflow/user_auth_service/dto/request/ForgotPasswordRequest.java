package com.tripflow.user_auth_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Richiesta di recupero password: invia una mail con un codice OTP per il reset")
public record ForgotPasswordRequest(
        @Schema(description = "Indirizzo email dell'account", example = "mario.rossi@example.com")
        @NotBlank(message = "L'email è obbligatoria")
        @Email(message = "Formato email non valido")
        String email
) {
}
