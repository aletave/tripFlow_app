package com.tripflow.gateway.gatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


//TODO implementare DNSResolutionFixer nella fase di dockerizzazione
// Main del modulo gateway-service.
// @EnableDiscoveryClient: il gateway si registra su Eureka come gli altri micro
// e usa il registry per risolvere gli uri "lb://<service-name>" delle route.
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayServiceApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(GatewayServiceApplication.class).run(args);
    }
}
