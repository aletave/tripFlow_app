package com.tripflow.itinerary_service.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ShareItineraryRequest(
        @NotNull UUID sharedWithUserId,
        boolean canEdit
) {
}