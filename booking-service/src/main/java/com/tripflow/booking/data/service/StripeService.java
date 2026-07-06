package com.tripflow.booking.data.service;

import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.tripflow.booking.exception.BookingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.stripe.model.Charge;
import com.stripe.model.StripeObject;
import com.stripe.param.PaymentIntentRetrieveParams;
import com.tripflow.booking.data.entities.enums.MetodoPagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@Slf4j
public class StripeService {

    private final StripeClient stripeClient;
    private final String webhookSecret;

    public StripeService(StripeClient stripeClient,
                         @Value("${stripe.api.webhook-secret}") String webhookSecret) {
        this.stripeClient = stripeClient;
        this.webhookSecret = webhookSecret;
    }

    //crea un PaymentIntent su Stripe. Usato da avviaPagamento.
    public PaymentIntent creaPaymentIntent(BigDecimal importoEuro, UUID prenotazioneId) {

        //setScale(2) prima della conversione: se mai arrivasse un prezzo con
        //più di 2 decimali, longValueExact() lancerebbe ArithmeticException.
        long importoCentesimi = importoEuro
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(importoCentesimi)
                .setCurrency("eur")
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build())
                .putMetadata("prenotazione_id", prenotazioneId.toString())
                .build();

        try {
            return stripeClient.paymentIntents().create(params);
        } catch (StripeException e) {
            log.error("Errore Stripe creando il PaymentIntent per prenotazione {}: {}",
                    prenotazioneId, e.getMessage());
            throw new BookingException("Impossibile avviare il pagamento presso Stripe");
        }
    }

    public Event costruisciEvento(String payload, String signatureHeader) {
        try {
            return Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Firma webhook Stripe non valida: {}", e.getMessage());
            throw new BookingException("Firma del webhook non valida");
        }
    }

    public String estraiPaymentIntentId(Event event) {
        StripeObject obj = event.getDataObjectDeserializer().getObject()
                .orElseThrow(() -> new BookingException(
                        "Impossibile deserializzare l'evento Stripe: " + event.getType()));

        if (obj instanceof PaymentIntent paymentIntent) {
            return paymentIntent.getId();
        }
        throw new BookingException(
                "Evento Stripe inatteso, l'oggetto non è un PaymentIntent: " + event.getType());
    }

    public DettagliCarta recuperaDettagliCarta(String paymentIntentId) {
        try {
            PaymentIntentRetrieveParams params = PaymentIntentRetrieveParams.builder()
                    .addExpand("latest_charge")
                    .build();

            PaymentIntent pi = stripeClient.paymentIntents().retrieve(paymentIntentId, params);
            Charge charge = pi.getLatestChargeObject();

            if (charge == null
                    || charge.getPaymentMethodDetails() == null
                    || charge.getPaymentMethodDetails().getCard() == null) {
                return DettagliCarta.vuoto();
            }

            Charge.PaymentMethodDetails.Card card = charge.getPaymentMethodDetails().getCard();
            return new DettagliCarta(card.getBrand(), card.getLast4(), mappaMetodo(card.getFunding()));

        } catch (StripeException e) {
            log.error("Errore Stripe recuperando i dettagli carta per {}: {}",
                    paymentIntentId, e.getMessage());
            return DettagliCarta.vuoto();
        }
    }

    private MetodoPagamento mappaMetodo(String funding) {
        if (funding == null) return null;
        return switch (funding) {
            case "credit" -> MetodoPagamento.CARTA_CREDITO;
            case "debit"  -> MetodoPagamento.CARTA_DEBITO;
            default       -> null; // prepaid / unknown
        };
    }

    public record DettagliCarta(String brand, String ultime4, MetodoPagamento metodo) {
        static DettagliCarta vuoto() {
            return new DettagliCarta(null, null, null);
        }
    }
}