package com.tripflow.user_auth_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Cambio password dell'utente autenticato: richiede la password attuale per conferma")
public record ChangePasswordRequest(
        @Schema(description = "Password attuale, per verifica", example = "VecchiaPassword1!")
        @NotBlank(message = "La password attuale è obbligatoria")
        String currentPassword,

        @Schema(description = "Nuova password (min 8 caratteri)", example = "NuovaPassword1!")
        @NotBlank(message = "La nuova password è obbligatoria")
        @Size(min = 8, max = 72, message = "La password deve essere tra 8 e 72 caratteri")
        String newPassword
) {
}
