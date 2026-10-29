package com.tripflow.itinerary_service.exception;

/**
 * Manca un'autenticazione valida o il token non contiene un'identita' utilizzabile.
 *
 * <p>Diventa 401, non 403: il client deve rinnovare la sessione, non chiedere altri permessi.
 */
public class UnauthorizedActionException extends RuntimeException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
