package unical.it.user_auth_service.dto;

import java.time.LocalDate;
import unical.it.user_auth_service.model.Role;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        LocalDate dateOfBirth,
        String phoneNumber,
        String profileImage
) {
}
