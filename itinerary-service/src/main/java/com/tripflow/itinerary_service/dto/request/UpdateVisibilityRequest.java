package com.tripflow.itinerary_service.dto.request;

import com.tripflow.itinerary_service.model.Visibility;
import jakarta.validation.constraints.NotNull;

public record UpdateVisibilityRequest(
        @NotNull Visibility visibility
) {
}