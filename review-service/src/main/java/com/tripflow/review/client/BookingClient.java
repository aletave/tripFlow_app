package com.tripflow.review.client;

import com.tripflow.review.client.dto.PrenotazioneResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

// Client REST verso booking.
// L'header Authorization (Bearer) viene propagato automaticamente da FeignAuthInterceptor,
// così booking valida lo stesso JWT e ricava da sé il viaggiatore (per il check ownership).
@FeignClient(name = "booking-service", url = "${tripflow.booking.base-url}")
public interface BookingClient {

    @GetMapping("/api/prenotazioni/{id}")
    PrenotazioneResponseDTO getPrenotazione(@PathVariable("id") UUID id);
}