package com.tripflow.user_auth_service.dto.response;

import java.time.LocalDate;
import java.util.UUID;
import com.tripflow.user_auth_service.model.Role;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Role role,
        LocalDate dateOfBirth,
        String phoneNumber,
        String profileImage
) {
}
