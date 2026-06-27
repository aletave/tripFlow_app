package com.tripflow.booking.data.service;

import com.tripflow.booking.data.dto.responses.PagamentoIntentResponse;
import com.tripflow.booking.data.dto.responses.PagamentoResponse;

import java.util.UUID;

public interface PagamentoService {

    PagamentoIntentResponse avviaPagamento(UUID prenotazioneId, UUID viaggiatoreId);
    PagamentoResponse trovaPagamento(UUID prenotazioneId, UUID viaggiatoreId);

    //stripe (chiamati dal webhook controller)
    PagamentoResponse confermaPagamento(String stripePaymentIntentId);
    PagamentoResponse gestisciPagamentoFallito(String stripePaymentIntentId);

    // rimborso (chiamato internamente da PrenotazioneService.annullaPrenotazione)
    PagamentoResponse rimborsaPagamento(UUID prenotazioneId);
}