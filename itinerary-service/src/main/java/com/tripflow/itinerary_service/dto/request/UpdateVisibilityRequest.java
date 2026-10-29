package com.tripflow.itinerary_service.dto.request;

import com.tripflow.itinerary_service.model.Visibility;
import jakarta.validation.constraints.NotNull;

/**
 * Nuova visibilita' dell'itinerario: {@code PRIVATE}, {@code SHARED} o {@code PUBLIC}.
 */
public record UpdateVisibilityRequest(
        @NotNull(message = "La visibilita' e' obbligatoria")
        Visibility visibility
) {
}
