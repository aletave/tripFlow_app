package com.tripflow.user_auth_service.dto.events;

import java.util.UUID;

public record UserDeletedMessageDTO(
        UUID userId,
        String keycloakId,
        String email
) {
}
