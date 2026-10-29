package com.tripflow.catalog_service.service;

import com.tripflow.catalog_service.config.RabbitmqConfig;
import com.tripflow.catalog_service.dto.EventDTO;
import com.tripflow.catalog_service.dto.BookingMessageDTO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class TripEventConsumer {

    private final TripService tripService;
    private final ActivityService activityService;

    public TripEventConsumer(TripService tripService, ActivityService activityService) {
        this.tripService = tripService;
        this.activityService = activityService;
    }

    @RabbitListener(queues = RabbitmqConfig.CATALOG_BOOKING_QUEUE)
    public void handleBookingEvent(EventDTO<BookingMessageDTO> event) {
        System.out.println("[RABBITMQ CONSUMER] Ricevuto evento: " + event.getEventType());

        BookingMessageDTO payload = event.getPayload();

        try {
            if ("BOOKING_CONFIRMED".equals(event.getEventType())) {
                tripService.decreaseAvailableSpots(payload.getTripId(), payload.getSpots());
                activityService.addBookedSpots(payload.getActivityIds(), payload.getSpots());
            } else if ("BOOKING_CANCELLED".equals(event.getEventType())) {
                tripService.increaseAvailableSpots(payload.getTripId(), payload.getSpots());
                activityService.releaseBookedSpots(payload.getActivityIds(), payload.getSpots());
            }
        } catch (Exception e) {
            System.err.println("[RABBITMQ SAGA ERROR] Errore nell'aggiornamento dei posti: " + e.getMessage());
        }
    }
}
