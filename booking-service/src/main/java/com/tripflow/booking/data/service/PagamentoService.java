package com.tripflow.booking.data.service;

import com.tripflow.booking.data.dto.requests.PagamentoRequest;
import com.tripflow.booking.data.dto.responses.PagamentoResponse;

import java.util.UUID;

public interface PagamentoService {

    PagamentoResponse avviaPagamento(UUID prenotazioneId, UUID viaggiatoreId, PagamentoRequest request);
    PagamentoResponse trovaPagamento(UUID prenotazioneId, UUID viaggiatoreId);

    //stripe (chiamati dal webhook controller)
    PagamentoResponse confermaPagamento(String stripePaymentIntentId);
    PagamentoResponse gestisciPagamentoFallito(String stripePaymentIntentId);

    // rimborso (chiamato internamente da PrenotazioneService.annullaPrenotazione)
    PagamentoResponse rimborsaPagamento(UUID prenotazioneId);
}