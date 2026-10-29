package com.tripflow.itinerary_service.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Vista di un itinerario {@code PUBLIC} per chi lo apre tramite token
 * ({@code GET /public/{token}}).
 *
 * <p>Contiene solo i dati da mostrare: niente proprietario, visibilita', token,
 * permessi o timestamp tecnici.
 */
public record PublicItineraryResponse(

        Long id,

        String title,

        String description,

        LocalDate startDate,

        LocalDate endDate,

        List<ItineraryStopResponse> stops

) {
}
