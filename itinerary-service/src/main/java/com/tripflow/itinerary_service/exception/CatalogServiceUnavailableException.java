package com.tripflow.itinerary_service.exception;

// Sollevata quando catalog-service non risponde o risponde con un errore inatteso.
// Diversa da "viaggio inesistente" (che e' un errore del client, 400): un guasto del
// catalogo non deve far credere all'utente di aver inserito dati sbagliati.
public class CatalogServiceUnavailableException extends RuntimeException {

    public CatalogServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
