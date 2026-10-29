package com.tripflow.catalog_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidActivitySpotsException extends RuntimeException {

    public InvalidActivitySpotsException(String message) {
        super(message);
    }
}

