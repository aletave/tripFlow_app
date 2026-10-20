package com.tripflow.user_auth_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Richiesta di lookup batch per Keycloak ID (il claim 'sub' del JWT)")
public record UserLookupByKeycloakIdsRequest(
        @Schema(description = "Lista di Keycloak ID (sub) di cui recuperare i profili pubblici")
        @NotEmpty @Size(max = 100) List<String> keycloakIds
) {
}
