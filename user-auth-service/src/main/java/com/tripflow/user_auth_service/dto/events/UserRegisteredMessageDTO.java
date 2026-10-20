package com.tripflow.user_auth_service.dto.events;

import com.tripflow.user_auth_service.model.Role;

import java.util.UUID;

public record UserRegisteredMessageDTO(
        UUID userId,
        String keycloakId,
        String email,
        Role role,
        String firstName,
        String lastName
) {
}
