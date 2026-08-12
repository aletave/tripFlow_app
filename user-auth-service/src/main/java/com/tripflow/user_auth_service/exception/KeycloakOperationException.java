package com.tripflow.user_auth_service.exception;

public class KeycloakOperationException extends RuntimeException {

    public KeycloakOperationException(String message) {
        super(message);
    }

    public KeycloakOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}