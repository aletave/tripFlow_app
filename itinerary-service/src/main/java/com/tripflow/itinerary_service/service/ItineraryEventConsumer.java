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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ascolta gli eventi del catalogo su RabbitMQ e tiene allineate le tappe.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItineraryEventConsumer {

    private final ItineraryStopRepository itineraryStopRepository;

    /**
     * Gestisce {@code TRIP_DELETED}: elimina le tappe che puntano al viaggio cancellato
     * e quelle che puntano alle sue attivita' (cancellate insieme al viaggio).
     *
     * <p>Il payload e' il viaggio del catalogo, con la lista {@code activities} e i loro id.
     * Eventi di altro tipo o senza id valido vengono ignorati. In caso di errore il messaggio
     * viene ritentato (vedi {@code spring.rabbitmq.listener.simple.retry}).
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitmqConfig.ITINERARY_TRIP_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitmqConfig.TRIP_EXCHANGE, type = "topic"),
            key = "trip.deleted"
    ))
    @Transactional
    public void handleTripDeletedEvent(EventDTO<Map<String, Object>> event) {
        log.info("[RABBITMQ CONSUMER] Ricevuto evento Itinerary: {}", event.getEventType());

        if (!"TRIP_DELETED".equals(event.getEventType()) || event.getPayload() == null) {
            return;
        }

        UUID tripId = toUuid(event.getPayload().get("id"));
        if (tripId == null) {
            log.error("Evento TRIP_DELETED senza id valido del viaggio: {}", event.getPayload());
            return;
        }

        Set<UUID> activityIds = extractActivityIds(event.getPayload().get("activities"));

        log.warn("Il Viaggio {} e' stato eliminato dal Catalogo. Eliminazione tappe orfane in corso...", tripId);

        int tripStops = itineraryStopRepository.deleteByViaggioId(tripId);
        int activityStops = activityIds.isEmpty() ? 0 : itineraryStopRepository.deleteByAttivitaIdIn(activityIds);

        log.info("Pulizia completata per il viaggio {}: {} tappe TRIP e {} tappe ACTIVITY eliminate",
                tripId, tripStops, activityStops);
    }

    private Set<UUID> extractActivityIds(Object activities) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (activities instanceof Collection<?> list) {
            for (Object activity : list) {
                if (activity instanceof Map<?, ?> map) {
                    UUID id = toUuid(map.get("id"));
                    if (id != null) {
                        ids.add(id);
                    }
                }
            }
        }
        return ids;
    }

    private UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            log.error("Id non valido nell'evento: {}", value);
            return null;
        }
    }
}
