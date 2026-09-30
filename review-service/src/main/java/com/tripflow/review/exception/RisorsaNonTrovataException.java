package com.tripflow.review.exception;

import jakarta.persistence.EntityNotFoundException;

//Risorsa inesistente su un altro servizio (prenotazione su booking,
//viaggio/attivita' su catalog): gestita come 404 dall'handler di EntityNotFoundException
public class RisorsaNonTrovataException extends EntityNotFoundException {
    public RisorsaNonTrovataException(String message) {
        super(message);
    }
}
