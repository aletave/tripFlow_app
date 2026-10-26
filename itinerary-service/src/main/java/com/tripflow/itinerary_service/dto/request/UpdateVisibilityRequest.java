package com.tripflow.itinerary_service.dto.request;

import com.tripflow.itinerary_service.model.Visibility;

public record UpdateVisibilityRequest(
        Visibility visibility,
        Boolean isPublic
) {
}
