package com.tripflow.booking.exception;

import jakarta.persistence.EntityNotFoundException;

import java.util.UUID;

public class PrenotazioneNotFoundException extends EntityNotFoundException {

    public PrenotazioneNotFoundException(UUID id) {
        super("Prenotazione non trovata con id: " + id);
    }

    public PrenotazioneNotFoundException(String message) {
        super(message);
    }
}
