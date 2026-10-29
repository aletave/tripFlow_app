package com.tripflow.itinerary_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Busta generica degli eventi RabbitMQ: tipo dell'evento (es. {@code TRIP_DELETED})
 * e contenuto. I campi non previsti vengono ignorati.
 *
 * @param <T> tipo del contenuto
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDTO<T> {
    private String eventType;
    private T payload;
}
