package com.tripflow.itinerary_service.exception;

/**
 * user-auth-service non e' raggiungibile o risponde con un errore inatteso (diventa 503).
 *
 * <p>Diversa da {@link ResourceNotFoundException}: un guasto dell'altro servizio non va
 * confuso con "utente inesistente" (404).
 */
public class UserAuthServiceUnavailableException extends RuntimeException {

    public UserAuthServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
