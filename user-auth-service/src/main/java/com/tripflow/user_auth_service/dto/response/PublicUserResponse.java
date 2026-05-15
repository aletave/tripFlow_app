package com.tripflow.user_auth_service.dto.response;

import java.util.UUID;
import com.tripflow.user_auth_service.model.Role;

public record PublicUserResponse(
        UUID id,
        String firstName,
        String lastName,
        Role role,
        String profileImage
) {
}
