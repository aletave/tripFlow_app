package com.tripflow.user_auth_service.exception;

//Login bloccato perché l'indirizzo email non è ancora stato verificato con il codice OTP.
//Mappata su 403 dal GlobalExceptionHandler per distinguerla dalle credenziali errate (401).
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
