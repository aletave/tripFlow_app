package com.tripflow.review.exception;

import jakarta.persistence.EntityNotFoundException;

public class RisorsaNonTrovataException extends EntityNotFoundException {
    public RisorsaNonTrovataException(String message) {
        super(message);
    }
}
