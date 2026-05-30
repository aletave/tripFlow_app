package com.tripflow.booking.data.service.events;

import java.util.UUID;

/**
 * Evento pubblicato quando un pagamento è andato a buon fine.
 *
 * Pubblicato da: PagamentoServiceImpl.confermaPagamento()
 *                (chiamato a sua volta dal webhook Stripe).
 * Listener:      PrenotazioneServiceImpl.onPagamentoCompletato()
 *                che porta la prenotazione da IN_ATTESA a CONFERMATA.
 *
 */
public record PagamentoCompletatoEvent(UUID prenotazioneId) {
}