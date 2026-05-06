package com.tripflow.itinerary_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateItineraryRequest(

        @NotBlank
        @Size(max = 150)
        String title,

        String description,

        @NotBlank
        @Size(max = 30)
        String visibility,

        LocalDate startDate,

        LocalDate endDate

) {
}