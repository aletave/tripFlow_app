package com.tripflow.itinerary_service.dto.errors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Corpo JSON di ogni risposta di errore del servizio
 * (timestamp, codice HTTP, descrizione, messaggio, percorso).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceError {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
