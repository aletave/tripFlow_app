package com.tripflow.catalog_service.mapper;

import com.tripflow.catalog_service.dto.request.ActivityRequestDTO;
import com.tripflow.catalog_service.dto.response.ActivityResponseDTO;
import com.tripflow.catalog_service.model.Activity;
import org.springframework.stereotype.Component;

@Component
public class ActivityMapper {

    public ActivityResponseDTO toResponseDTO(Activity activity) {
        if (activity == null) {
            return null;
        }

        ActivityResponseDTO dto = new ActivityResponseDTO();
        dto.setId(activity.getId());
        dto.setName(activity.getName());
        dto.setDescription(activity.getDescription());
        dto.setDuration(activity.getDuration());
        dto.setPrice(activity.getPrice());
        dto.setAvailableSpots(activity.getAvailableSpots());

        if (activity.getTrip() != null) {
            dto.setTripId(activity.getTrip().getId());
        }
        return dto;
    }


    public Activity toEntity(ActivityRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }

        Activity activity = new Activity();
        activity.setName(requestDTO.getName());
        activity.setDescription(requestDTO.getDescription());
        activity.setDuration(requestDTO.getDuration());
        activity.setPrice(requestDTO.getPrice());
        activity.setAvailableSpots(requestDTO.getAvailableSpots());

        return activity;
    }
}