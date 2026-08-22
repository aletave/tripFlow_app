package com.tripflow.itinerary_service.controller;

import com.tripflow.itinerary_service.configSecurity.SecurityUtils;
import com.tripflow.itinerary_service.dto.request.AddItineraryStopRequest;
import com.tripflow.itinerary_service.dto.request.CreateItineraryRequest;
import com.tripflow.itinerary_service.dto.response.ItineraryResponse;
import com.tripflow.itinerary_service.dto.response.ItineraryStopResponse;
import com.tripflow.itinerary_service.service.ItineraryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controller REST che espone gli endpoint HTTP per itinerari e tappe.
// L'owner non arriva più dai parametri client: è sempre il subject del JWT.
@RestController
@RequestMapping("/api/itineraries")
public class ItineraryController {

    private final ItineraryService itineraryService;

    // Dependency Injection del service
    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    // Crea un nuovo itinerario per l'utente autenticato
    @PostMapping
    public ItineraryResponse createItinerary(
            @Valid @RequestBody CreateItineraryRequest request
    ) {
        return itineraryService.createItinerary(request, SecurityUtils.ownerId());
    }

    // Recupera un itinerario tramite id (solo se appartiene all'utente autenticato)
    @GetMapping("/{id}")
    public ItineraryResponse getItineraryById(@PathVariable Long id) {
        return itineraryService.getItineraryById(id, SecurityUtils.ownerId());
    }

    // Recupera tutti gli itinerari dell'utente autenticato
    @GetMapping("/mie")
    public List<ItineraryResponse> getItinerariesByOwner() {
        return itineraryService.getItinerariesByOwner(SecurityUtils.ownerId());
    }

    // Aggiunge una tappa a un itinerario (solo se appartiene all'utente autenticato)
    @PostMapping("/{id}/stops")
    public ItineraryStopResponse addStopToItinerary(
            @PathVariable Long id,
            @Valid @RequestBody AddItineraryStopRequest request
    ) {
        return itineraryService.addStopToItinerary(id, request, SecurityUtils.ownerId());
    }

    // Elimina un itinerario (solo se appartiene all'utente autenticato)
    @DeleteMapping("/{id}")
    public void deleteItinerary(@PathVariable Long id) {
        itineraryService.deleteItinerary(id, SecurityUtils.ownerId());
    }
}
