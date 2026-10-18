package com.tripflow.itinerary_service.exception;

// Sollevata quando user-auth-service risponde con un errore inatteso o non e' raggiungibile.
// E' diversa da ResourceNotFoundException: un guasto dell'altro servizio non deve
// essere confuso con "utente inesistente" (che invece e' un 404).
public class UserAuthServiceUnavailableException extends RuntimeException {

    public UserAuthServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
