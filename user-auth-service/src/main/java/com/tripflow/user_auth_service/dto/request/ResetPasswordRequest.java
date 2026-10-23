package com.tripflow.user_auth_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Reset della password tramite codice OTP ricevuto via mail")
public record ResetPasswordRequest(
        @Schema(description = "Indirizzo email dell'account", example = "mario.rossi@example.com")
        @NotBlank(message = "L'email è obbligatoria")
        @Email(message = "Formato email non valido")
        String email,

        @Schema(description = "Codice OTP di 6 cifre ricevuto via email", example = "123456")
        @NotBlank(message = "Il codice è obbligatorio")
        @Pattern(regexp = "\\d{6}", message = "Il codice deve essere di 6 cifre")
        String code,

        @Schema(description = "Nuova password (min 8 caratteri)", example = "NuovaPassword1!")
        @NotBlank(message = "La nuova password è obbligatoria")
        @Size(min = 8, max = 72, message = "La password deve essere tra 8 e 72 caratteri")
        String newPassword
) {
}
