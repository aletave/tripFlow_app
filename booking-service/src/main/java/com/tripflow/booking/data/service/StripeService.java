package com.tripflow.booking.data.service;

import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.model.Coupon;
import com.stripe.model.PromotionCode;
import com.stripe.model.StripeCollection;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentUpdateParams;
import com.stripe.param.PromotionCodeListParams;
import com.tripflow.booking.exception.BookingException;
import com.tripflow.booking.exception.CodiceScontoNonValidoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.stripe.model.Charge;
import com.stripe.model.StripeObject;
import com.stripe.param.PaymentIntentRetrieveParams;
import com.tripflow.booking.data.entities.enums.MetodoPagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class StripeService {

    private final StripeClient stripeClient;
    private final String webhookSecret;
    private final String publishableKey;

    public StripeService(StripeClient stripeClient,
                         @Value("${stripe.api.webhook-secret}") String webhookSecret,
                         @Value("${stripe.api.publishable-key}") String publishableKey) {
        this.stripeClient = stripeClient;
        this.webhookSecret = webhookSecret;
        this.publishableKey = publishableKey;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public PaymentIntent recuperaPaymentIntent(String paymentIntentId) {
        try {
            return stripeClient.paymentIntents().retrieve(paymentIntentId);
        } catch (StripeException e) {
            log.error("Errore Stripe recuperando il PaymentIntent {}: {}", paymentIntentId, e.getMessage());
            throw new BookingException("Impossibile recuperare il pagamento presso Stripe");
        }
    }

    //crea un PaymentIntent su Stripe. Usato da avviaPagamento
    public PaymentIntent creaPaymentIntent(BigDecimal importoEuro, UUID prenotazioneId, String codiceSconto) {

        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                .setAmount(inCentesimi(importoEuro))
                .setCurrency("eur")
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build())
                .putMetadata("prenotazione_id", prenotazioneId.toString());
        if (codiceSconto != null) {
            builder.putMetadata("codice_sconto", codiceSconto);
        }
        PaymentIntentCreateParams params = builder.build();

        try {
            return stripeClient.paymentIntents().create(params);
        } catch (StripeException e) {
            log.error("Errore Stripe creando il PaymentIntent per prenotazione {}: {}",
                    prenotazioneId, e.getMessage());
            throw new BookingException("Impossibile avviare il pagamento presso Stripe");
        }
    }

    public PaymentIntent aggiornaImportoPaymentIntent(String paymentIntentId, BigDecimal importoEuro, String codiceSconto) {

        PaymentIntentUpdateParams params = PaymentIntentUpdateParams.builder()
                .setAmount(inCentesimi(importoEuro))
                .putMetadata("codice_sconto", valoreMetadata(codiceSconto))
                .build();

        try {
            return stripeClient.paymentIntents().update(paymentIntentId, params);
        } catch (StripeException e) {
            log.error("Errore Stripe aggiornando l'importo del PaymentIntent {}: {}",
                    paymentIntentId, e.getMessage());
            throw new BookingException("Impossibile aggiornare l'importo del pagamento presso Stripe");
        }
    }

    public CodiceSconto recuperaCodiceSconto(String codice){

        PromotionCodeListParams params = PromotionCodeListParams.builder()
                .setCode(codice)
                .setActive(true)
                .setLimit(1L)
                .addExpand("data.promotion.coupon")
                .build();

        StripeCollection<PromotionCode> risultati;
        try {
            risultati = stripeClient.promotionCodes().list(params);
        } catch (StripeException e) {
            log.error("Errore Stripe cercando il codice sconto {}: {}", codice, e.getMessage());
            throw new BookingException("Impossibile verificare il codice sconto presso Stripe");
        }

        if(risultati.getData().isEmpty()){
            throw new CodiceScontoNonValidoException("Codice sconto non valido o non più attivo");
        }

        PromotionCode promotionCode = risultati.getData().get(0);

        Long scadenza = promotionCode.getExpiresAt();
        if(scadenza != null && scadenza <= Instant.now().getEpochSecond()) {
            throw new CodiceScontoNonValidoException("Codice sconto scaduto");
        }

        Coupon coupon = null;
        if (promotionCode.getPromotion() != null) {
            coupon = promotionCode.getPromotion().getCouponObject();
        }
        if (coupon == null || !Boolean.TRUE.equals(coupon.getValid())) {
            throw new CodiceScontoNonValidoException("Codice sconto non valido o non più attivo");
        }
        if (coupon.getPercentOff() == null) {
            throw new CodiceScontoNonValidoException("Tipo di codice sconto non supportato");
        }

        return new CodiceSconto(promotionCode.getId(), promotionCode.getCode(), coupon.getPercentOff());
    }

    private long inCentesimi(BigDecimal importoEuro) {
        return importoEuro
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
    }

    private String valoreMetadata(String valore) {
        if (valore == null) {
            return "";
        }
        return valore;
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

    public record CodiceSconto(String promotionCodeId, String codice, BigDecimal percentuale) {
    }

    public record DettagliCarta(String brand, String ultime4, MetodoPagamento metodo) {
        static DettagliCarta vuoto() {
            return new DettagliCarta(null, null, null);
        }
    }
}