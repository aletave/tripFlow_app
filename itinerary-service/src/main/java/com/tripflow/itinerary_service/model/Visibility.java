package com.tripflow.itinerary_service.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tripflow.itinerary_service.config.VisibilityDeserializer;

/**
 * Chi puo' vedere un itinerario.
 * <ul>
 *   <li>{@code PRIVATE}: solo il proprietario;</li>
 *   <li>{@code SHARED}: il proprietario e gli utenti con cui e' condiviso;</li>
 *   <li>{@code PUBLIC}: tutti gli utenti, anche tramite token pubblico.</li>
 * </ul>
 */
@JsonDeserialize(using = VisibilityDeserializer.class)
public enum Visibility {
    PUBLIC,
    PRIVATE,
    SHARED
}
