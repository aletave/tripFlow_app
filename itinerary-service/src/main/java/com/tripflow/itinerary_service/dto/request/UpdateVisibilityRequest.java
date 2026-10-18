package com.tripflow.itinerary_service.dto.request;

import com.tripflow.itinerary_service.model.Visibility;

// Come per CreateItineraryRequest, accettiamo sia l'enum "visibility"
// sia il booleano "isPublic". Default PRIVATE se entrambi assenti.
public record UpdateVisibilityRequest(
        Visibility visibility,
        Boolean isPublic
) {

    public Visibility resolvedVisibility() {
        if (visibility != null) {
            return visibility;
        }
        if (isPublic != null) {
            return isPublic ? Visibility.PUBLIC : Visibility.PRIVATE;
        }
        return Visibility.PRIVATE;
    }
}
