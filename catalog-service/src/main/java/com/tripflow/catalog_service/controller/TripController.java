package com.tripflow.catalog_service.controller;


import com.tripflow.catalog_service.dto.request.TripRequestDTO;
import com.tripflow.catalog_service.dto.response.TripResponseDTO;
import com.tripflow.catalog_service.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "Trips", description = "Endpoint per la gestione dei viaggi")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    private UUID getCurrentUserId() {
        String id = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Permette di creare un nuovo viaggio", description = "Salva il viaggio nel database e pubblica un evento tramite rabbitmq")
    public ResponseEntity<TripResponseDTO> createTrip(@Valid @RequestBody TripRequestDTO requestDTO) {

        UUID organizerId = getCurrentUserId();
        TripResponseDTO createdTrip = tripService.createTrip(requestDTO, organizerId);

        return new ResponseEntity<>(createdTrip, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Permette di trovare un viaggio tramite l'id", description = "Cerca un viaggio nel database con una query avente l'id specificaato")
    public ResponseEntity<TripResponseDTO> getTripById(@PathVariable UUID id) {

        TripResponseDTO trip = tripService.getTripById(id);

        return ResponseEntity.ok(trip);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Permette di aggiornare i dettagli di un viaggio", description = "Aggiorna un viaggio nel db con una query che aggiorna i dettagli forniti")
    public ResponseEntity<TripResponseDTO> updateTrip(@PathVariable UUID id, @Valid @RequestBody TripRequestDTO requestDTO) {

        UUID organizerId = getCurrentUserId();
        TripResponseDTO updatedTrip = tripService.updateTrip(id, requestDTO, organizerId);


        return ResponseEntity.ok(updatedTrip);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Permette di eliminare un viaggio", description = "Elimina un dato viaggio nel db")
    public ResponseEntity<Void> deleteTrip(@PathVariable UUID id) {

        UUID organizerId = getCurrentUserId();
        tripService.deleteTrip(id, organizerId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/organizer/{organizerId}")
    @Operation(summary = "Permette di trovare un viaggio tramite l'id dell'organizzatore", description = "Cerca un viaggio nel database con una query avente l'id dell'organizzatore")
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
