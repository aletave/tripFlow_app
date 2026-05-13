package com.tripflow.catalog_service.controller;


import com.tripflow.catalog_service.dto.request.ActivityRequestDTO;
import com.tripflow.catalog_service.dto.response.ActivityResponseDTO;
import com.tripflow.catalog_service.service.ActivityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    public ResponseEntity<ActivityResponseDTO> createActivity(@Valid @RequestBody ActivityRequestDTO requestDTO) {

        ActivityResponseDTO createdActivity = activityService.createActivity(requestDTO);

        return new ResponseEntity<>(createdActivity, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityResponseDTO> getActivityById(@PathVariable UUID id) {
        ActivityResponseDTO activity = activityService.getActivityById(id);

        return ResponseEntity.ok(activity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActivityResponseDTO> updateActivity(@PathVariable UUID id, @Valid @RequestBody ActivityRequestDTO requestDTO) {

        ActivityResponseDTO updatedAct = activityService.updateActivity(id, requestDTO);

        return ResponseEntity.ok(updatedAct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable UUID id) {
        activityService.deleteActivity(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trip/{tripId}")
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



//PROVA






