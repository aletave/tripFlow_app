package com.tripflow.itinerary_service.exception;

// Sollevata quando manca un'autenticazione valida o il token non contiene
// un'identita' utilizzabile. Restituisce 401, non 403: il client deve
// rinnovare la sessione, non cambiare permessi.
public class UnauthorizedActionException extends RuntimeException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
