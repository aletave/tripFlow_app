package com.tripflow.review.client;

import com.tripflow.review.client.dto.PrenotazioneResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;


@FeignClient(name = "booking-service")
public interface BookingClient {

    @GetMapping("/api/prenotazioni/{id}")
    PrenotazioneResponseDTO getPrenotazione(@PathVariable("id") UUID id);
}