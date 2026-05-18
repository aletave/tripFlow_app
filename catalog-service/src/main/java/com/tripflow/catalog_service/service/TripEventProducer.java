package com.tripflow.catalog_service.service;

import com.tripflow.catalog_service.config.RabbitmqConfig;
import com.tripflow.catalog_service.dto.EventDTO;
import com.tripflow.catalog_service.dto.response.TripResponseDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class TripEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public TripEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate=rabbitTemplate;
    }

    public void sendTripCreatedEvent(TripResponseDTO t) {
        EventDTO<TripResponseDTO> event= new EventDTO<>("TRIP_CREATED", t);
        String key = "trip.created";
        rabbitTemplate.convertAndSend(RabbitmqConfig.EXCHANGE_NAME, key, event);

        System.out.println("[RABBITMQ] evento inviato: " + event.getEventType() + " a " + t.getDestination());

    }
}
