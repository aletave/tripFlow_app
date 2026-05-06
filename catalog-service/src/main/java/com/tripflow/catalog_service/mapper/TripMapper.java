package com.tripflow.catalog_service.mapper;

import com.tripflow.catalog_service.dto.request.TripRequestDTO;
import com.tripflow.catalog_service.dto.response.ActivityResponseDTO;
import com.tripflow.catalog_service.dto.response.TripResponseDTO;
import com.tripflow.catalog_service.model.Trip;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TripMapper {

    private final ActivityMapper activityMapper;

    public TripMapper(ActivityMapper activityMapper) {
        this.activityMapper = activityMapper;
    }

    public TripResponseDTO toResponseDTO(Trip trip) {
        if (trip == null) {
            return null;
        }

        TripResponseDTO dto = new TripResponseDTO();
        dto.setId(trip.getId());
        dto.setName(trip.getName());
        dto.setDestination(trip.getDestination());
        dto.setStartDate(trip.getStartDate());
        dto.setEndDate(trip.getEndDate());
        dto.setPrice(trip.getPrice());
        dto.setAvailableSpots(trip.getAvailableSpots());
        dto.setOrganizerId(trip.getOrganizerId());
        dto.setDescription(trip.getDescription());
        dto.setImages(trip.getImages());

        if (trip.getActivities() != null) {
            List<ActivityResponseDTO> activityDTOs = trip.getActivities().stream()
                    .map(activityMapper::toResponseDTO)
                    .collect(Collectors.toList());
            dto.setActivities(activityDTOs);
        }

        return dto;
    }

    public Trip toEntity(TripRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }

        Trip trip = new Trip();
        trip.setName(requestDTO.getName());
        trip.setDestination(requestDTO.getDestination());
        trip.setStartDate(requestDTO.getStartDate());
        trip.setEndDate(requestDTO.getEndDate());
        trip.setPrice(requestDTO.getPrice());
        trip.setAvailableSpots(requestDTO.getAvailableSpots());
        trip.setDescription(requestDTO.getDescription());
        trip.setImages(requestDTO.getImages());

        return trip;
    }
}