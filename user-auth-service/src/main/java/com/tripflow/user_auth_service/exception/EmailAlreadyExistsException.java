package com.tripflow.user_auth_service.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email già in uso: " + email);
    }

    public EmailAlreadyExistsException() {
        super("Email già in uso");
    }
}
