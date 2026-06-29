package com.tripflow.booking.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class RabbitmqConfig {

    public static final String BOOKING_EXCHANGE = "booking-exchange";

    public static final String TRIP_EXCHANGE = "trip-exchange";

    public static final String BOOKING_TRIP_QUEUE = "booking-trip-queue";

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE);
    }

    @Bean
    public TopicExchange tripExchange() {
        return new TopicExchange(TRIP_EXCHANGE);
    }

    @Bean
    public Queue bookingTripQueue() {
        return new Queue(BOOKING_TRIP_QUEUE, true);
    }

    @Bean
    public Binding bindingTripDeleted(Queue bookingTripQueue, TopicExchange tripExchange) {
        return BindingBuilder.bind(bookingTripQueue).to(tripExchange).with("trip.deleted");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
