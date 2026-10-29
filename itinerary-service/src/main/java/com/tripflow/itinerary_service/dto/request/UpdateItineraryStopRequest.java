package com.tripflow.itinerary_service.dto.request;

import com.tripflow.itinerary_service.validation.DateRange;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modifica parziale di una tappa: i campi {@code null} restano invariati.
 * Se cambia {@code stopType}, i riferimenti al catalogo vengono sostituiti.
 */
@DateRange(startField = "startDatetime", endField = "endDatetime")
public record UpdateItineraryStopRequest(

        Integer stopOrder,

        @Size(max = 30)
        String stopType,

        UUID viaggioId,

        UUID attivitaId,

        @Size(max = 150)
        String customTitle,

        @Size(max = 150)
        String customDescription,

        LocalDateTime startDatetime,

        LocalDateTime endDatetime,

        String notes

) {
}