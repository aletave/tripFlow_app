package com.tripflow.itinerary_service.dto.response;

import java.time.LocalDateTime;

//restituirà un oggetto con all'interno i seguenti dati
public record ItineraryStopResponse(

        Long id,

        Integer stopOrder,

        String stopType,

        Long viaggioId,

        Long attivitaId,

        String customTitle,

        String customDescription,

        LocalDateTime startDatetime,

        LocalDateTime endDatetime,

        String notes

) {
}