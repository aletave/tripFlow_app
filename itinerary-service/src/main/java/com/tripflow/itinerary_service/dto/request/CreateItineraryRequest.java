package com.tripflow.itinerary_service.dto.request;

import com.tripflow.itinerary_service.model.Visibility;
import com.tripflow.itinerary_service.validation.DateRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@DateRange(startField = "startDate", endField = "endDate")
public record CreateItineraryRequest(

        @NotBlank
        @Size(max = 150)
        String title,

        String description,

        Visibility visibility,

        Boolean isPublic,

        LocalDate startDate,

        LocalDate endDate

) {
}
