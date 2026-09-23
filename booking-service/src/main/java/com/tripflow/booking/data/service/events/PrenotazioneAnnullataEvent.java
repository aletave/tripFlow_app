package com.tripflow.booking.data.service.events;

import java.util.UUID;

//evento pubblicato quando una prenotazione viene annullata
public record PrenotazioneAnnullataEvent(
        UUID prenotazioneId,
        boolean eraConfermata
) {
}
