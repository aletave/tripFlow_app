package com.tripflow.booking.data.service;

import com.tripflow.booking.config.RabbitmqConfig;
import com.tripflow.booking.data.dao.PrenotazioneRepository;
import com.tripflow.booking.data.messages.EventDTO;
import com.tripflow.booking.data.entities.Prenotazione;
import com.tripflow.booking.data.entities.enums.StatoPrenotazione;
import com.tripflow.booking.data.service.events.PrenotazioneAnnullataEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final PrenotazioneRepository prenotazioneRepository;
    private final ApplicationEventPublisher eventPublisher;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitmqConfig.BOOKING_TRIP_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitmqConfig.TRIP_EXCHANGE, type = "topic"),
            key = "trip.deleted"
    ))
    @Transactional
    public void handleTripDeletedEvent(EventDTO<java.util.Map<String, Object>> event) {
        log.info("[RABBITMQ CONSUMER] Ricevuto evento dal catalogo: {}", event.getEventType());

        if ("TRIP_DELETED".equals(event.getEventType())) {
            String idString = (String) event.getPayload().get("id");
            UUID tripId = UUID.fromString(idString);

            log.warn("[SAGA] Il Viaggio {} è stato eliminato dal catalogo. Annullamento prenotazioni in corso...", tripId);

            List<Prenotazione> prenotazioni = prenotazioneRepository.findByViaggioId(tripId);
            int annullate = 0;
            for (Prenotazione p : prenotazioni) {
                StatoPrenotazione stato = p.getStato();


                if (stato == StatoPrenotazione.ANNULLATA
                        || stato == StatoPrenotazione.COMPLETATA) {
                    continue;
                }

                boolean eraConfermata = (stato == StatoPrenotazione.CONFERMATA);

                p.setStato(StatoPrenotazione.ANNULLATA);
                prenotazioneRepository.save(p);
                annullate++;


                eventPublisher.publishEvent(
                        new PrenotazioneAnnullataEvent(p.getId(), eraConfermata));
            }
            log.info("[SAGA] Completato. Annullate {} prenotazioni per il viaggio {}", annullate, tripId);
        }
    }
}
