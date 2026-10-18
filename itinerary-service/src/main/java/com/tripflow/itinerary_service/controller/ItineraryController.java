package com.tripflow.itinerary_service.controller;

import com.tripflow.itinerary_service.configSecurity.SecurityUtils;
import com.tripflow.itinerary_service.dto.request.AddItineraryStopRequest;
import com.tripflow.itinerary_service.dto.request.CreateItineraryRequest;
import com.tripflow.itinerary_service.dto.request.ShareItineraryRequest;
import com.tripflow.itinerary_service.dto.request.UpdateItineraryStopRequest;
import com.tripflow.itinerary_service.dto.request.UpdateVisibilityRequest;
import com.tripflow.itinerary_service.dto.response.ItineraryResponse;
import com.tripflow.itinerary_service.dto.response.ItineraryShareResponse;
import com.tripflow.itinerary_service.dto.response.ItineraryStopResponse;
import com.tripflow.itinerary_service.service.ItineraryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controller REST che espone gli endpoint HTTP per itinerari e tappe.
// Livelli di autorizzazione:
// - GET /{id}: owner o utente con condivisione (read) o PUBLIC
// - POST /{id}/stops, PUT /{id}/stops/{stopId}: owner o utente con condivisione e can_edit=true
// - DELETE /{id}/stops/{stopId}: owner o utente con condivisione e can_edit=true
// - PATCH /{id}/visibility: solo owner
// - PUT /{id}: owner o utente con condivisione e can_edit=true
// - DELETE /{id}, shares management: solo owner
// - GET /public/{id}: accesso pubblico (solo PUBLIC visibility)
@RestController
@RequestMapping("/api/itineraries")
public class ItineraryController {

    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @PostMapping
    public ItineraryResponse createItinerary(
            @Valid @RequestBody CreateItineraryRequest request
    ) {
        return itineraryService.createItinerary(request, SecurityUtils.ownerId());
    }

    @GetMapping
    public List<ItineraryResponse> getAllItineraries() {
        return itineraryService.getAllAccessibleItineraries(SecurityUtils.ownerId());
    }

    @GetMapping("/{id}")
    public ItineraryResponse getItineraryById(@PathVariable Long id) {
        return itineraryService.getItineraryById(id, SecurityUtils.ownerId());
    }

    @GetMapping("/public/{id}")
    public ItineraryResponse getPublicItinerary(@PathVariable Long id) {
        return itineraryService.getPublicItinerary(id);
    }

    @GetMapping("/mie")
    public List<ItineraryResponse> getItinerariesByOwner() {
        return itineraryService.getItinerariesByOwner(SecurityUtils.ownerId());
    }

    @PostMapping("/{id}/stops")
    public ItineraryStopResponse addStopToItinerary(
            @PathVariable Long id,
            @Valid @RequestBody AddItineraryStopRequest request
    ) {
        return itineraryService.addStopToItinerary(id, request, SecurityUtils.ownerId());
    }

    @PutMapping("/{id}/stops/{stopId}")
    public ItineraryStopResponse updateStop(
            @PathVariable Long id,
            @PathVariable Long stopId,
            @Valid @RequestBody UpdateItineraryStopRequest request
    ) {
        return itineraryService.updateStop(id, stopId, request, SecurityUtils.ownerId());
    }

    @DeleteMapping("/{id}/stops/{stopId}")
    public void deleteStop(
            @PathVariable Long id,
            @PathVariable Long stopId
    ) {
        itineraryService.deleteStop(id, stopId, SecurityUtils.ownerId());
    }

    @PatchMapping("/{id}/visibility")
    public ItineraryResponse updateVisibility(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVisibilityRequest request
    ) {
        return itineraryService.updateVisibility(id, request, SecurityUtils.ownerId());
    }

    @PutMapping("/{id}")
    public ItineraryResponse updateItinerary(
            @PathVariable Long id,
            @Valid @RequestBody CreateItineraryRequest request
    ) {
        return itineraryService.updateItinerary(id, request, SecurityUtils.ownerId());
    }

    @DeleteMapping("/{id}")
    public void deleteItinerary(@PathVariable Long id) {
        itineraryService.deleteItinerary(id, SecurityUtils.ownerId());
    }

    @PostMapping("/{id}/shares")
    public ItineraryShareResponse shareItinerary(
            @PathVariable Long id,
            @Valid @RequestBody ShareItineraryRequest request
    ) {
        return itineraryService.shareItinerary(id, request, SecurityUtils.ownerId());
    }

    @DeleteMapping("/{id}/shares/{sharedWithUserId}")
    public void removeShare(
            @PathVariable Long id,
            @PathVariable java.util.UUID sharedWithUserId
    ) {
        itineraryService.removeShare(id, sharedWithUserId, SecurityUtils.ownerId());
    }

    @GetMapping("/{id}/shares")
    public List<ItineraryShareResponse> getShares(@PathVariable Long id) {
        return itineraryService.getShares(id, SecurityUtils.ownerId());
    }
}
