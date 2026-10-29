package com.tripflow.itinerary_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Avvio di itinerary-service: microservizio che gestisce itinerari, tappe e condivisioni.
 * Si registra su Eureka per essere raggiunto tramite il gateway.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ItineraryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ItineraryServiceApplication.class, args);
    }
}