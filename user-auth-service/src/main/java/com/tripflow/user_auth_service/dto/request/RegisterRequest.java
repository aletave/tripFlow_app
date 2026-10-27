package com.tripflow.user_auth_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import com.tripflow.user_auth_service.model.Role;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        @PastOrPresent LocalDate dateOfBirth,
        //Formato internazionale E.164: "+" e da 7 a 15 cifre, senza spazi (es. +393331234567).
        //Facoltativo: null lo lascia vuoto.
        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$",
                message = "deve essere in formato internazionale, es. +393331234567")
        @Size(max = 30) String phoneNumber
) {
}
