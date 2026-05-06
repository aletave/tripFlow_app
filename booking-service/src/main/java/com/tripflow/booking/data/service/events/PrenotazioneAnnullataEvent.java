package com.tripflow.booking.data.service.events;

import java.util.UUID;

/**
 * Evento pubblicato quando una prenotazione viene annullata.
 *
 * Listener: PagamentoEventListener (in PagamentoService impl).
 * Azione conseguente: se la prenotazione era CONFERMATA (e quindi pagata),
 * il listener avvia il rimborso del pagamento associato.
 *
 */
public record PrenotazioneAnnullataEvent(
        UUID prenotazioneId,
        boolean eraConfermata
) {
}
