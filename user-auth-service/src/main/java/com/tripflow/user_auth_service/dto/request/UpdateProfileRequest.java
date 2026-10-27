package com.tripflow.user_auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @PastOrPresent LocalDate dateOfBirth,
        //Formato internazionale E.164: "+" e da 7 a 15 cifre, senza spazi (es. +393331234567).
        //Facoltativo: null lo lascia vuoto.
        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$",
                message = "deve essere in formato internazionale, es. +393331234567")
        @Size(max = 30) String phoneNumber
) {
}
