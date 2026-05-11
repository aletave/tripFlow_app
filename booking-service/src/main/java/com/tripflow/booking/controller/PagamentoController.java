package com.tripflow.booking.controller;

import com.tripflow.booking.data.dto.requests.PagamentoRequest;
import com.tripflow.booking.data.dto.responses.PagamentoResponse;
import com.tripflow.booking.data.service.PagamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/pagamenti")
@RequiredArgsConstructor
@Slf4j
public class PagamentoController {

    private final PagamentoService pagamentoService;

    private static final String HEADER_VIAGGIATORE = "X-Viaggiatore-Id";

    //Operazioni viaggiatore

    @PostMapping("/{prenotazioneId}")
    @ResponseStatus(HttpStatus.CREATED)
    public PagamentoResponse avvia(
            @PathVariable UUID prenotazioneId,
            @RequestHeader(HEADER_VIAGGIATORE) UUID viaggiatoreId,
            @Valid @RequestBody PagamentoRequest request) {
        return pagamentoService.avviaPagamento(prenotazioneId, viaggiatoreId, request);
    }

    @GetMapping("/{prenotazioneId}")
    public PagamentoResponse trova(
            @PathVariable UUID prenotazioneId,
            @RequestHeader(HEADER_VIAGGIATORE) UUID viaggiatoreId) {
        return pagamentoService.trovaPagamento(prenotazioneId, viaggiatoreId);
    }

    //Webhook
    // TODO: validare la firma Stripe (header Stripe-Signature)
    // TODO: unificare in /webhook con dispatch sul tipo evento
    // ============================================

    @PostMapping("/webhook/successo")
    public PagamentoResponse onSuccesso(@RequestParam String paymentIntentId) {
        return pagamentoService.confermaPagamento(paymentIntentId);
    }

    @PostMapping("/webhook/fallimento")
    public PagamentoResponse onFallimento(@RequestParam String paymentIntentId) {
        return pagamentoService.gestisciPagamentoFallito(paymentIntentId);
    }
}