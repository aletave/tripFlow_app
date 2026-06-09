package com.tripflow.discovery.discoveryservice;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

// Main del modulo discovery-service.
// @EnableEurekaServer trasforma questa app in un Eureka Server standalone:
// gli altri micro (catalog, booking, review, gateway) si registreranno qui
// all'avvio chiamando http://localhost:8761/eureka/.
// La dashboard web è raggiungibile su http://localhost:8761
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(DiscoveryServiceApplication.class).run(args);
    }
}