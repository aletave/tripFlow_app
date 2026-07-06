package com.tripflow.booking.data.entities.enums;

public enum StatoPagamento {
    IN_ATTESA,
    COMPLETATO,
    FALLITO,
    RIMBORSATO
}

//TODO: Stripe non rimborserà i pagamenti, valutare successivamete di rimuovere stato RIMBORSATO