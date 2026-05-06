package com.tripflow.catalog_service.service;

import com.tripflow.catalog_service.dto.request.ActivityRequestDTO;
import com.tripflow.catalog_service.dto.response.ActivityResponseDTO;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ActivityService {

    ActivityResponseDTO createActivity(ActivityRequestDTO requestDTO);

    ActivityResponseDTO getActivityById(UUID id);

    ActivityResponseDTO updateActivity(UUID id, ActivityRequestDTO requestDTO);

    void deleteActivity(UUID id);

    List<ActivityResponseDTO> getActivitiesByTripId(UUID tripId);

    List<ActivityResponseDTO> searchActivities(String name, String description, Integer duration,
                                               BigDecimal minPrice, BigDecimal maxPrice, Integer minAvailableSpots);
}