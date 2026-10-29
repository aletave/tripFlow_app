package com.tripflow.itinerary_service.dto.response;

import com.tripflow.itinerary_service.model.Visibility;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Itinerario completo con tappe, come lo riceve l'app.
 *
 * <p>{@code publicToken} e' valorizzato solo se l'itinerario e' {@code PUBLIC};
 * {@code canEdit} dice se l'utente corrente puo' modificarlo. I timestamp sono in UTC.
 */
public record ItineraryResponse(

        Long id,

        UUID ownerId,

        String title,

        String description,

        Visibility visibility,

        LocalDate startDate,

        LocalDate endDate,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt,

        List<ItineraryStopResponse> stops,

        UUID publicToken,

        boolean canEdit

) {
}