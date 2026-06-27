package com.tripflow.catalog_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitmqConfig {

    public static final String EXCHANGE_NAME = "trip-exchange";

    @Bean
    public TopicExchange tripExchange() {

        return new TopicExchange(EXCHANGE_NAME);
    }

    public static final String BOOKING_EXCHANGE = "booking-exchange";
    public static final String CATALOG_BOOKING_QUEUE = "catalog-booking_queue";

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE);
    }

    @Bean
    public Queue catalogBookingQueue() {
        return new Queue(CATALOG_BOOKING_QUEUE, true);
    }

    @Bean
    public Binding bindingBookingConfirmed(Queue catalogBookingQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(catalogBookingQueue).to(bookingExchange).with("booking.confirmed");
    }

    @Bean
    public Binding bindingBookingCancelled(Queue catalogBookingQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(catalogBookingQueue).to(bookingExchange).with("booking.cancelled");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {

        return new Jackson2JsonMessageConverter();
    }
}
