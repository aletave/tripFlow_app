package com.tripflow.itinerary_service.dto.request;

import com.tripflow.itinerary_service.validation.DateRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Nuovi titolo, descrizione e date dell'itinerario (sostituiscono i precedenti).
 */
@DateRange(startField = "startDate", endField = "endDate")
public record UpdateItineraryRequest(

        @NotBlank
        @Size(max = 150)
        String title,

        String description,

        LocalDate startDate,

        LocalDate endDate

) {
}
