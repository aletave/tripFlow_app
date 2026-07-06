package com.tripflow.itinerary_service.service;

import com.tripflow.itinerary_service.config.RabbitmqConfig;
import com.tripflow.itinerary_service.dto.EventDTO;
import com.tripflow.itinerary_service.repository.ItineraryStopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItineraryEventConsumer {

    private final ItineraryStopRepository itineraryStopRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitmqConfig.ITINERARY_TRIP_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitmqConfig.TRIP_EXCHANGE, type = "topic"),
            key = "trip.deleted"
    ))
    @Transactional
    public void handleTripDeletedEvent(EventDTO<java.util.Map<String, Object>> event) {
        log.info("[RABBITMQ CONSUMER] Ricevuto evento Itinerary: {}", event.getEventType());

        if ("TRIP_DELETED".equals(event.getEventType())) {
            String idString = (String) event.getPayload().get("id");
            UUID tripId = UUID.fromString(idString);

            log.warn("Il Viaggio {} è stato eliminato dal Catalogo. Eliminazione tappe orfane in corso...", tripId);

            try {
                itineraryStopRepository.deleteByViaggioId(tripId);
                log.info("Pulizia completata! Tappe eliminate per il viaggio {}", tripId);
            } catch (Exception e) {
                log.error("Errore durante l'eliminazione delle tappe: {}", e.getMessage());
            }
        }
    }
}
