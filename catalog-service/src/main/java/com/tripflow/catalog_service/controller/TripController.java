package com.tripflow.catalog_service.controller;


import com.tripflow.catalog_service.dto.request.TripRequestDTO;
import com.tripflow.catalog_service.dto.response.TripResponseDTO;
import com.tripflow.catalog_service.service.TripService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    public ResponseEntity<TripResponseDTO> createTrip(@Valid @RequestBody TripRequestDTO requestDTO, UUID organizerId) {
        TripResponseDTO createdTrip = tripService.createTrip(requestDTO, organizerId);

        return new ResponseEntity<>(createdTrip, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponseDTO> getTripById(@PathVariable UUID id, TripService tripService) {

        TripResponseDTO trip = tripService.getTripById(id);

        return ResponseEntity.ok(trip);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripResponseDTO> updateTrip(@PathVariable UUID id, @Valid @RequestBody TripRequestDTO requestDTO,
                                                      UUID organizerId) {
        TripResponseDTO updatedTrip = tripService.updateTrip(id, requestDTO, organizerId);

        return ResponseEntity.ok(updatedTrip);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable UUID id, UUID organizerId) {
        tripService.deleteTrip(id, organizerId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<List<TripResponseDTO>> getTripsByOrganizerId(@PathVariable UUID organizerId) {
        List<TripResponseDTO> trips = tripService.getTripsByOrganizer(organizerId);

        return ResponseEntity.ok(trips);
    }

    @GetMapping("/search")
    public ResponseEntity<List<TripResponseDTO>> searchTrips(@RequestParam(required = false) String destination,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                             @RequestParam(required = false) BigDecimal minPrice,
                                                             @RequestParam(required = false) BigDecimal maxPrice,
                                                             @RequestParam(required = false) Integer minAvailableSpots) {

        List<TripResponseDTO> trips = tripService.searchTrips(destination, startDate, endDate, minPrice, maxPrice, minAvailableSpots);

        return ResponseEntity.ok(trips);
    }
}
