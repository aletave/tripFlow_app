package com.tripflow.booking.data.service;

import com.tripflow.booking.config.RabbitmqConfig;
import com.tripflow.booking.data.dao.PrenotazioneRepository;
import com.tripflow.booking.data.messages.EventDTO;
import com.tripflow.booking.data.entities.Prenotazione;
import com.tripflow.booking.data.entities.enums.StatoPrenotazione;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final PrenotazioneRepository prenotazioneRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitmqConfig.BOOKING_TRIP_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitmqConfig.TRIP_EXCHANGE, type = "topic"),
            key = "trip.deleted"
    ))
    @Transactional
    public void handleTripDeletedEvent(EventDTO<UUID> event) {
        log.info("[RABBITMQ CONSUMER] Ricevuto evento: {}", event.getEventType());

        if ("TRIP_DELETED".equals(event.getEventType())) {
            UUID tripId = event.getPayload();
            log.warn("Il Viaggio {} è stato eliminato. Annullamento prenotazioni...", tripId);

            List<Prenotazione> prenotazioni = prenotazioneRepository.findByViaggioId(tripId);

            int annullate = 0;
            for (Prenotazione p : prenotazioni) {
                if (p.getStato() != StatoPrenotazione.ANNULLATA) {
                    p.setStato(StatoPrenotazione.ANNULLATA);
                    prenotazioneRepository.save(p);
                    annullate++;
                }
            }
            log.info("Annullate {} prenotazioni per il viaggio {}", annullate, tripId);
        }
    }
}
