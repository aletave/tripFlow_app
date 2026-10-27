package com.tripflow.itinerary_service.dto.response;

import java.time.LocalDate;
import java.util.List;

// Vista di un itinerario PUBLIC esposta tramite link (GET /public/{token}).
// Contiene solo i dati destinati a chi riceve il link: niente ownerId,
// visibility, publicToken, canEdit o timestamp tecnici.
public record PublicItineraryResponse(

        Long id,

        String title,

        String description,

        LocalDate startDate,

        LocalDate endDate,

        List<ItineraryStopResponse> stops

) {
}
