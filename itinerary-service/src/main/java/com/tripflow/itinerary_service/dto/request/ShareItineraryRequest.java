package com.tripflow.itinerary_service.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Condivisione con un utente.
 *
 * @param sharedWithUserId id Keycloak dell'utente destinatario
 * @param canEdit se {@code true} l'utente puo' anche modificare tappe e dati
 */
public record ShareItineraryRequest(
        @NotNull UUID sharedWithUserId,
        boolean canEdit
) {
}