package unical.it.user_auth_service.dto;

import unical.it.user_auth_service.model.Role;

public record AuthResponse(
        Long id,
        String email,
        Role role,
        String token
) {
}
