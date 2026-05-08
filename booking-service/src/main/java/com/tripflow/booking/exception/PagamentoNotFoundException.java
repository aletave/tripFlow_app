package com.tripflow.booking.exception;

import jakarta.persistence.EntityNotFoundException;

public class PagamentoNotFoundException extends EntityNotFoundException {

    public PagamentoNotFoundException(String message) {
        super(message);
    }
}
