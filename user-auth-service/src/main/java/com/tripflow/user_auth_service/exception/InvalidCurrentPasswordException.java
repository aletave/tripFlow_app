package com.tripflow.user_auth_service.exception;

//Password attuale errata nel cambio password. Mappata su 400 e non su 401: l'utente è
//autenticato, e un 401 farebbe credere al client che la sessione sia scaduta.
public class InvalidCurrentPasswordException extends RuntimeException {
    public InvalidCurrentPasswordException(String message) {
        super(message);
    }
}
