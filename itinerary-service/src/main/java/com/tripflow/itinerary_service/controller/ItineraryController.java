package com.tripflow.itinerary_service.controller;

import com.tripflow.itinerary_service.dto.request.AddItineraryStopRequest;
import com.tripflow.itinerary_service.dto.request.CreateItineraryRequest;
import com.tripflow.itinerary_service.dto.response.ItineraryResponse;
import com.tripflow.itinerary_service.dto.response.ItineraryStopResponse;
import com.tripflow.itinerary_service.service.ItineraryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controller REST che espone gli endpoint HTTP per itinerari e tappe
@RestController
@RequestMapping("/api/itineraries")
public class ItineraryController {

    private final ItineraryService itineraryService;

    // Dependency Injection del service
    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    // Crea un nuovo itinerario
    @PostMapping
    public ItineraryResponse createItinerary(
            @Valid @RequestBody CreateItineraryRequest request,
            @RequestParam Long ownerId
    ) {
        return itineraryService.createItinerary(request, ownerId);
    }

    // Recupera un itinerario tramite id
    @GetMapping("/{id}")
    public ItineraryResponse getItineraryById(@PathVariable Long id) {
        return itineraryService.getItineraryById(id);
    }

    // Recupera tutti gli itinerari di un owner
    @GetMapping("/owner/{ownerId}")
    public List<ItineraryResponse> getItinerariesByOwner(@PathVariable Long ownerId) {
        return itineraryService.getItinerariesByOwner(ownerId);
    }

    // Aggiunge una tappa a un itinerario
    @PostMapping("/{id}/stops")
    public ItineraryStopResponse addStopToItinerary(
            @PathVariable Long id,
            @Valid @RequestBody AddItineraryStopRequest request
    ) {
        return itineraryService.addStopToItinerary(id, request);
    }

    // Elimina un itinerario
    @DeleteMapping("/{id}")
    public void deleteItinerary(@PathVariable Long id) {
        itineraryService.deleteItinerary(id);
    }
}