package com.tripflow.itinerary_service.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tripflow.itinerary_service.config.VisibilityDeserializer;

@JsonDeserialize(using = VisibilityDeserializer.class)
public enum Visibility {
    PUBLIC,
    PRIVATE,
    SHARED
}
