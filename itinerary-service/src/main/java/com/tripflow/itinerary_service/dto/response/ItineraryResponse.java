package com.tripflow.itinerary_service.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

//restituirà un oggetto con all'interno i seguenti dati
public record ItineraryResponse(

        Long id,

        Long ownerId,

        String title,

        String description,

        String visibility,

        LocalDate startDate,

        LocalDate endDate,

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        List<ItineraryStopResponse> stops

) {
}