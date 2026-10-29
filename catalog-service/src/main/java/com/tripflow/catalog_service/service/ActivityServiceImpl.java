package com.tripflow.catalog_service.service;

import com.tripflow.catalog_service.dto.request.ActivityRequestDTO;
import com.tripflow.catalog_service.dto.response.ActivityResponseDTO;
import com.tripflow.catalog_service.exception.InvalidActivitySpotsException;
import com.tripflow.catalog_service.exception.ResourceNotFoundException;
import com.tripflow.catalog_service.model.Activity;
import com.tripflow.catalog_service.model.Trip;
import com.tripflow.catalog_service.repository.ActivityRepository;
import com.tripflow.catalog_service.repository.TripRepository;
import com.tripflow.catalog_service.repository.ActivitySpecification;
import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final TripRepository tripRepository;
    private final ModelMapper modelMapper;

    public ActivityServiceImpl(ActivityRepository activityRepository,
                               TripRepository tripRepository,
                               ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
        this.activityRepository = activityRepository;
        this.tripRepository = tripRepository;
    }

    @Override
    @Transactional
    public ActivityResponseDTO createActivity(ActivityRequestDTO requestDTO, UUID organizerId) {
        Trip trip = tripRepository.findById(requestDTO.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("viaggio non trovato con id: " + requestDTO.getTripId()));

        if (!trip.getOrganizerId().equals(organizerId)) {
            throw new AccessDeniedException("Non hai i permessi per aggiungere attività a questo viaggio");
        }

        // I posti liberi netti del viaggio = availableSpots - bookedSpots
        int booked = trip.getBookedSpots() != null ? trip.getBookedSpots() : 0;
        int freeSpots = trip.getAvailableSpots() - booked;

        if (requestDTO.getAvailableSpots() > freeSpots) {
            throw new InvalidActivitySpotsException(
                    "Il numero di posti dell'attività (" + requestDTO.getAvailableSpots() +
                    ") non può superare i posti liberi del viaggio (" + freeSpots + ")");
        }

        Activity activity = modelMapper.map(requestDTO, Activity.class);
        activity.setTrip(trip);

        Activity savedActivity = activityRepository.save(activity);

        return modelMapper.map(savedActivity, ActivityResponseDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityResponseDTO getActivityById(UUID id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("attività non trovata con id: " + id));

        return modelMapper.map(activity, ActivityResponseDTO.class);
    }

    @Override
    @Transactional
    public ActivityResponseDTO updateActivity(UUID id, ActivityRequestDTO requestDTO, UUID organizerId) {
        Activity existingActivity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("attività non trovata con id: " + id));

        if (!existingActivity.getTrip().getOrganizerId().equals(organizerId)) {
            throw new AccessDeniedException("Non hai i permessi per modificare questa attività");
        }

        modelMapper.map(requestDTO, existingActivity);

        Activity updatedActivity = activityRepository.save(existingActivity);

        return modelMapper.map(updatedActivity, ActivityResponseDTO.class);
    }

    @Override
    @Transactional
    public void deleteActivity(UUID id, UUID organizerId) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attività non trovata con id: " + id));

        if (!activity.getTrip().getOrganizerId().equals(organizerId)) {
            throw new AccessDeniedException("Non hai i permessi per eliminare questa attività");
        }

        activityRepository.delete(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponseDTO> getActivitiesByTripId(UUID tripId) {
        List<Activity> activities = activityRepository.findByTripId(tripId);
        return activities.stream()
                .map(activity -> modelMapper.map(activity, ActivityResponseDTO.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponseDTO> searchActivities(String name, String description, Integer duration,
                                                      BigDecimal minPrice, BigDecimal maxPrice, Integer minAvailableSpots) {

        Specification<Activity> spec = ActivitySpecification.getFilterSpecification(
                name, description, duration, minPrice, maxPrice, minAvailableSpots);

        List<Activity> activities = activityRepository.findAll(spec);

        return activities.stream()
                .map(activity -> modelMapper.map(activity, ActivityResponseDTO.class))
                .toList();
    }
}
