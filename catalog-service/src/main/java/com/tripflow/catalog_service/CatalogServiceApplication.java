package com.tripflow.catalog_service;

import com.tripflow.catalog_service.dto.response.TripResponseDTO;
import com.tripflow.catalog_service.service.TripEventProducer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import java.math.BigDecimal;
import java.util.UUID;

@SpringBootApplication
@EnableDiscoveryClient
public class CatalogServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatalogServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner testRabbitMQProducer(TripEventProducer producer) { return args -> {
			System.out.println("test rabbitmq in corso...");

			TripResponseDTO t = new TripResponseDTO();
			t.setId(UUID.randomUUID());
			t.setDestination("Bivona, Vibo Valentia (test rabbitmq)");
			t.setPrice(BigDecimal.valueOf(9999));

			producer.sendTripCreatedEvent(t);
		};
	}
}


