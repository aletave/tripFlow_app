package com.tripflow.review.exception;

import jakarta.persistence.EntityNotFoundException;

import java.util.UUID;

public class RecensioneNotFoundException extends EntityNotFoundException {

    public RecensioneNotFoundException(UUID id) {
        super("Recensione non trovata con id: " + id);
    }

    public RecensioneNotFoundException(String message) {
        super(message);
    }
}