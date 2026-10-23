package com.tripflow.user_auth_service.exception;

//Codice OTP inesistente, errato, già usato, scaduto o con troppi tentativi falliti.
//Mappata su 400 dal GlobalExceptionHandler. Il messaggio è volutamente generico
//per non rivelare quale delle condizioni si è verificata (né se l'email esiste).
public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException(String message) {
        super(message);
    }
}
