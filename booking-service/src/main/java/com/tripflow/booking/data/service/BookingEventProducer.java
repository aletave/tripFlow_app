package com.tripflow.booking.data.service;

import com.tripflow.booking.config.RabbitmqConfig;
import com.tripflow.booking.data.dao.PrenotazioneRepository;
import com.tripflow.booking.data.messages.BookingMessageDTO;
import com.tripflow.booking.data.messages.EventDTO;
import com.tripflow.booking.data.entities.Prenotazione;
import com.tripflow.booking.data.service.events.PagamentoCompletatoEvent;
import com.tripflow.booking.data.service.events.PrenotazioneAnnullataEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventProducer {

    private final RabbitTemplate rabbitTemplate;
    private final PrenotazioneRepository prenotazioneRepository;

    @EventListener
    public void handlePagamentoCompletato(PagamentoCompletatoEvent event) {
        Prenotazione prenotazione = prenotazioneRepository.findById(event.prenotazioneId())
                .orElseThrow(() -> new RuntimeException("Prenotazione non trovata"));

        BookingMessageDTO payload = new BookingMessageDTO(
                prenotazione.getViaggioId(),
                prenotazione.getNumeroPartecipanti()
        );

        EventDTO<BookingMessageDTO> rabbitEvent = new EventDTO<>("BOOKING_CONFIRMED", payload);

        rabbitTemplate.convertAndSend(
                RabbitmqConfig.BOOKING_EXCHANGE,
                "booking.confirmed",
                rabbitEvent
        );
        log.info("[RABBITMQ PRODUCER] Inviato booking.confirmed per viaggio {}", prenotazione.getViaggioId());
    }


    @EventListener
    public void handlePrenotazioneAnnullata(PrenotazioneAnnullataEvent event) {
        if (event.eraConfermata()) {
            Prenotazione prenotazione = prenotazioneRepository.findById(event.prenotazioneId())
                    .orElseThrow(() -> new RuntimeException("Prenotazione non trovata"));

            BookingMessageDTO payload = new BookingMessageDTO(
                    prenotazione.getViaggioId(),
                    prenotazione.getNumeroPartecipanti()
            );

            EventDTO<BookingMessageDTO> rabbitEvent = new EventDTO<>("BOOKING_CANCELLED", payload);

            rabbitTemplate.convertAndSend(
                    RabbitmqConfig.BOOKING_EXCHANGE,
                    "booking.cancelled",
                    rabbitEvent
            );
            log.info("[RABBITMQ PRODUCER] Inviato booking.cancelled per viaggio {}", prenotazione.getViaggioId());
        }
    }
}
