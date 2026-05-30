package com.tripflow.review.client;

import com.tripflow.review.client.dto.PrenotazioneResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

//Client REST verso booking
@FeignClient(name = "booking-service", url = "${tripflow.booking.base-url}")
public interface BookingClient {

    @GetMapping("/api/prenotazioni/{id}")
    PrenotazioneResponseDTO getPrenotazione(@PathVariable("id") UUID id,
                                            @RequestHeader("X-Viaggiatore-Id") UUID viaggiatoreId);
}