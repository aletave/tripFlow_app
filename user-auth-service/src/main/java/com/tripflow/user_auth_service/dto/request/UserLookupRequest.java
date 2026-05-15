package com.tripflow.user_auth_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserLookupRequest(
        @NotEmpty @Size(max = 100) List<@Email String> emails
) {
}
