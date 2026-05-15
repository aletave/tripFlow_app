package com.tripflow.user_auth_service.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import com.tripflow.user_auth_service.model.Role;

public record AuthResponse(
        UUID id,
        String email,
        Role role,
        String accessToken,
        LocalDateTime accessTokenExpiresAt,
        String refreshToken,
        LocalDateTime refreshTokenExpiresAt
) {
}
