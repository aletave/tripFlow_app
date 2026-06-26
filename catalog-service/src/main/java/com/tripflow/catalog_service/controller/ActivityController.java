package com.tripflow.catalog_service.controller;


import com.tripflow.catalog_service.dto.request.ActivityRequestDTO;
import com.tripflow.catalog_service.dto.response.ActivityResponseDTO;
import com.tripflow.catalog_service.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activities")
@Tag(name = "Activities", description = "Endpoint per la gestione delle attività")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    @Operation(summary = "Permette di creare una nuova attività", description = "Salva l'attività nel database e pubblica un evento tramite rabbitmq")
    public ResponseEntity<ActivityResponseDTO> createActivity(@Valid @RequestBody ActivityRequestDTO requestDTO) {

        ActivityResponseDTO createdActivity = activityService.createActivity(requestDTO);

        return new ResponseEntity<>(createdActivity, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Permette di trovare un'attività tramite l'id", description = "Cerca un'attiività nel database con una query avente l'id specificaato")
    public ResponseEntity<ActivityResponseDTO> getActivityById(@PathVariable UUID id) {
        ActivityResponseDTO activity = activityService.getActivityById(id);

        return ResponseEntity.ok(activity);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Permette di aggiornare i dettagli di un'attività", description = "Aggiorna un'attività nel db con una query che aggiorna i dettagli forniti")
    public ResponseEntity<ActivityResponseDTO> updateActivity(@PathVariable UUID id, @Valid @RequestBody ActivityRequestDTO requestDTO) {

        ActivityResponseDTO updatedAct = activityService.updateActivity(id, requestDTO);

        return ResponseEntity.ok(updatedAct);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Permette di eliminare un'attività", description = "Elimina una data attività nel db")
    public ResponseEntity<Void> deleteActivity(@PathVariable UUID id) {
        activityService.deleteActivity(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trip/{tripId}")
    @Operation(summary = "Permette di trovare un attività tramite l'id del viaggio", description = "Cerca nel database un'attività tramite una query con l'id del viaggio associato")
    public ResponseEntity<List<ActivityResponseDTO>> getActivitiesByTripId(@PathVariable UUID tripId) {

        List<ActivityResponseDTO> act = activityService.getActivitiesByTripId(tripId);

        return ResponseEntity.ok(act);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ActivityResponseDTO>> searchActivities(@RequestParam(required = false) String name,
                                                                      @RequestParam(required = false) String description,
                                                                      @RequestParam(required = false) Integer duration,
                                                                      @RequestParam(required = false) BigDecimal minPrice,
                                                                      @RequestParam(required = false) BigDecimal maxPrice,
                                                                      @RequestParam(required = false) Integer minAvailableSpots) {
        List<ActivityResponseDTO> act = activityService.searchActivities(name, description, duration, minPrice, maxPrice, minAvailableSpots);

        return ResponseEntity.ok(act);
    }
}










