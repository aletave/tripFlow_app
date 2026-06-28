package com.tripflow.booking.controller;

import com.stripe.model.Event;
import com.tripflow.booking.configSecurity.SecurityUtils;
import com.tripflow.booking.data.dto.responses.PagamentoIntentResponse;
import com.tripflow.booking.data.dto.responses.PagamentoResponse;
import com.tripflow.booking.data.service.PagamentoService;
import com.tripflow.booking.data.service.StripeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/pagamenti")
@RequiredArgsConstructor
@Slf4j
public class PagamentoController {

    private final PagamentoService pagamentoService;
    private final StripeService stripeService;

    //Operazioni viaggiatore (id dal JWT validato)

    @PostMapping("/{prenotazioneId}")
    @ResponseStatus(HttpStatus.CREATED)
    public PagamentoIntentResponse avvia(@PathVariable UUID prenotazioneId) {
        return pagamentoService.avviaPagamento(prenotazioneId, SecurityUtils.viaggiatoreId());
    }

    @GetMapping("/{prenotazioneId}")
    public PagamentoResponse trova(@PathVariable UUID prenotazioneId) {
        return pagamentoService.trovaPagamento(prenotazioneId, SecurityUtils.viaggiatoreId());
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        Event event = stripeService.costruisciEvento(payload, signature);

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                String paymentIntentId = stripeService.estraiPaymentIntentId(event);
                pagamentoService.confermaPagamento(paymentIntentId);
                log.info("Webhook: pagamento confermato per PaymentIntent {}", paymentIntentId);
            }
            case "payment_intent.payment_failed" -> {
                String paymentIntentId = stripeService.estraiPaymentIntentId(event);
                pagamentoService.gestisciPagamentoFallito(paymentIntentId);
                log.warn("Webhook: pagamento fallito per PaymentIntent {}", paymentIntentId);
            }
            default -> log.debug("Webhook: evento Stripe ignorato ({})", event.getType());
        }

        return ResponseEntity.ok("");
    }
}