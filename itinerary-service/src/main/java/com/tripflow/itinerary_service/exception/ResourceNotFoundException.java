package com.tripflow.itinerary_service.exception;

/**
 * La risorsa richiesta (itinerario, tappa, condivisione, utente) non esiste (diventa 404).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
