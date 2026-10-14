package com.tripflow.itinerary_service.dto.request;

import com.tripflow.itinerary_service.model.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateItineraryRequest(

        @NotBlank
        @Size(max = 150)
        String title,

        String description,

        @NotNull
        Visibility visibility,

        LocalDate startDate,

        LocalDate endDate

) {
}