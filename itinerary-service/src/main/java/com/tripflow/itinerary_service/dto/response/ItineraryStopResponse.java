package com.tripflow.itinerary_service.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

//restituirà un oggetto con all'interno i seguenti dati
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