package com.tripflow.itinerary_service.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ItineraryShareResponse(
        Long id,
        Long itineraryId,
        UUID sharedWithUserId,
        boolean canEdit,
        OffsetDateTime createdAt
) {
}