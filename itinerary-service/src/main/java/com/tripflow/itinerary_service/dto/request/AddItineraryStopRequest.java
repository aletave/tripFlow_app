package com.tripflow.itinerary_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

public record AddItineraryStopRequest(

        @NotNull
        Integer stopOrder,

        @NotBlank @Size(max = 30)
        String stopType, //(viaggio/attività)

        UUID viaggioId,

        UUID attivitaId,

        @NotBlank @Size(max = 150)
        String customTitle,

        @NotBlank @Size(max = 150)
        String customDescription,

        @NotBlank @Size(max = 150)
        LocalDateTime startDatetime,

        @NotBlank @Size(max = 150)
        LocalDateTime endDatetime,

        String notes

) {
}