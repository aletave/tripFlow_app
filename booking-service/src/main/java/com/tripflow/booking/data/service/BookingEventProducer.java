package com.tripflow.booking.data.service;

import com.tripflow.booking.config.RabbitmqConfig;
import com.tripflow.booking.data.dao.PrenotazioneRepository;
import com.tripflow.booking.data.messages.BookingMessageDTO;
import com.tripflow.booking.data.messages.EventDTO;
import com.tripflow.booking.data.entities.Prenotazione;
import com.tripflow.booking.data.service.events.PagamentoCompletatoEvent;
import com.tripflow.booking.data.service.events.PrenotazioneAnnullataEvent;
import com.tripflow.booking.exception.PrenotazioneNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Pubblica su RabbitMQ gli eventi di booking destinati al catalog.
 *
 * I listener sono @TransactionalEventListener(AFTER_COMMIT), NON @EventListener:
 * la pubblicazione avviene solo a transazione conclusa con successo. Così
 * (1) non si annuncia al catalog un pagamento che poi viene rollbackato, e
 * (2) un RabbitMQ spento non fa più fallire il webhook Stripe.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventProducer {

    private final RabbitTemplate rabbitTemplate;
    private final PrenotazioneRepository prenotazioneRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePagamentoCompletato(PagamentoCompletatoEvent event) {
        pubblica(event.prenotazioneId(), "BOOKING_CONFIRMED", "booking.confirmed");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePrenotazioneAnnullata(PrenotazioneAnnullataEvent event) {
        if (!event.eraConfermata()) {
            return;
        }
        pubblica(event.prenotazioneId(), "BOOKING_CANCELLED", "booking.cancelled");
    }

    private void pubblica(UUID prenotazioneId, String eventType, String routingKey) {

        Prenotazione prenotazione = prenotazioneRepository.findById(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        BookingMessageDTO payload = new BookingMessageDTO(
                prenotazione.getViaggioId(),
                prenotazione.getNumeroPartecipanti()
        );

        try {
            rabbitTemplate.convertAndSend(
                    RabbitmqConfig.BOOKING_EXCHANGE,
                    routingKey,
                    new EventDTO<>(eventType, payload)
            );
            log.info("[RABBITMQ PRODUCER] Inviato {} per viaggio {}",
                    routingKey, prenotazione.getViaggioId());

        } catch (AmqpException e) {
            log.error("[RABBITMQ PRODUCER] Invio {} FALLITO per prenotazione {} " +
                            "(RabbitMQ non raggiungibile?): il catalog non è stato notificato",
                    routingKey, prenotazioneId, e);
        }
    }
}