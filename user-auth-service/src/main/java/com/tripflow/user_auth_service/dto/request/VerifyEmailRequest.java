package com.tripflow.user_auth_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Verifica dell'email tramite il codice OTP ricevuto via mail")
public record VerifyEmailRequest(
        @Schema(description = "Indirizzo email dell'account", example = "mario.rossi@example.com")
        @NotBlank(message = "L'email è obbligatoria")
        @Email(message = "Formato email non valido")
        String email,

        @Schema(description = "Codice OTP di 6 cifre ricevuto via email", example = "123456")
        @NotBlank(message = "Il codice è obbligatorio")
        @Pattern(regexp = "\\d{6}", message = "Il codice deve essere di 6 cifre")
        String code
) {
}
