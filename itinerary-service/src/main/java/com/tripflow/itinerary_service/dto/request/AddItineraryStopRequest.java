package com.tripflow.itinerary_service.dto.request;

import com.tripflow.itinerary_service.validation.DateRange;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

@DateRange(startField = "startDatetime", endField = "endDatetime")
public record AddItineraryStopRequest(

        @NotNull
        Integer stopOrder,

        @NotNull @Size(max = 30)
        String stopType,

        UUID viaggioId,

        UUID attivitaId,

        @Size(max = 150)
        String customTitle,

        @Size(max = 150)
        String customDescription,

        @NotNull
        LocalDateTime startDatetime,

        @NotNull
        LocalDateTime endDatetime,

        String notes

) {
}