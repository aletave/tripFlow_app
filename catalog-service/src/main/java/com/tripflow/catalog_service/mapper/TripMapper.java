package com.tripflow.catalog_service.mapper;

import com.tripflow.catalog_service.dto.response.ActivityResponseDTO;
import com.tripflow.catalog_service.dto.response.TripResponseDTO;
import com.tripflow.catalog_service.model.Activity;
import com.tripflow.catalog_service.model.Trip;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TripMapper {

    /**
     * Mappa Trip -> TripResponseDTO calcolando i posti liberi netti.
     * availableSpots nel DTO = availableSpots (totali) - bookedSpots (prenotati)
     * totalSpots nel DTO     = availableSpots totali dichiarati dall'organizzatore
     */
    public TripResponseDTO toDTO(Trip trip) {
        if (trip == null) return null;

        TripResponseDTO dto = new TripResponseDTO();
        dto.setId(trip.getId());
        dto.setName(trip.getName());
        dto.setDestination(trip.getDestination());
        dto.setStartDate(trip.getStartDate());
        dto.setEndDate(trip.getEndDate());
        dto.setPrice(trip.getPrice());
        dto.setOrganizerId(trip.getOrganizerId());
        dto.setDescription(trip.getDescription());
        dto.setImages(trip.getImages());

        int booked = trip.getBookedSpots() != null ? trip.getBookedSpots() : 0;
        dto.setTotalSpots(trip.getAvailableSpots());
        dto.setAvailableSpots(trip.getAvailableSpots() - booked);

        List<Activity> activities = trip.getActivities();
        if (activities != null) {
            dto.setActivities(activities.stream().map(this::toActivityDTO).toList());
        }

        return dto;
    }

    private ActivityResponseDTO toActivityDTO(Activity activity) {
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
}

