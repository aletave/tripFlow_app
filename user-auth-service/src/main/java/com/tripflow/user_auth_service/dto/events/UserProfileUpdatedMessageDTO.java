package com.tripflow.user_auth_service.dto.events;

import java.util.UUID;

public record UserProfileUpdatedMessageDTO(
        UUID userId,
        String keycloakId,
        String firstName,
        String lastName,
        String profileImage
) {
}
