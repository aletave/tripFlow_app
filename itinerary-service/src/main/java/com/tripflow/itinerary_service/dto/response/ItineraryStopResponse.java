package com.tripflow.itinerary_service.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Una tappa dell'itinerario. Le date sono nell'ora locale indicata dall'utente.
 */
public record ItineraryStopResponse(

        Long id,

        Integer stopOrder,

        String stopType,

        UUID viaggioId,

        UUID attivitaId,

        String customTitle,

        String customDescription,

        LocalDateTime startDatetime,

        LocalDateTime endDatetime,

        String notes

) {
}