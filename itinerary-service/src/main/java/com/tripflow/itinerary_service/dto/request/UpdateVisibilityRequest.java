package com.tripflow.itinerary_service.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tripflow.itinerary_service.model.Visibility;
import jakarta.validation.constraints.AssertTrue;

public record UpdateVisibilityRequest(
        Visibility visibility,
        Boolean isPublic
) {

    @JsonIgnore
    @AssertTrue(message = "Almeno uno tra visibility e isPublic deve essere specificato")
    public boolean isAtLeastOneFieldPresent() {
        return visibility != null || isPublic != null;
    }
}
