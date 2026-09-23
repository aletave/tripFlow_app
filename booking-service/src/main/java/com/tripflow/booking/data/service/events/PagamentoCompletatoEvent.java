package com.tripflow.booking.data.service.events;

import java.util.UUID;

//evento pubblicato quando un pagamento è andato a buon fine
public record PagamentoCompletatoEvent(UUID prenotazioneId) {
}