package com.tripflow.catalog_service.service;

import com.tripflow.catalog_service.dto.request.ActivityRequestDTO;
import com.tripflow.catalog_service.dto.response.ActivityResponseDTO;
import com.tripflow.catalog_service.exception.ResourceNotFoundException;
import com.tripflow.catalog_service.model.Activity;
import com.tripflow.catalog_service.model.Trip;
import com.tripflow.catalog_service.repository.ActivityRepository;
import com.tripflow.catalog_service.repository.TripRepository;
import com.tripflow.catalog_service.repository.ActivitySpecification;
import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.domain.Specification;
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
        this.modelMapper=modelMapper;
        this.activityRepository=activityRepository;
        this.tripRepository=tripRepository;
    }

    @Override
    @Transactional
    public ActivityResponseDTO createActivity(ActivityRequestDTO requestDTO) {
        Trip trip = tripRepository.findById(requestDTO.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("viaggio non trovato con id: " + requestDTO.getTripId()));

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
    public ActivityResponseDTO updateActivity(UUID id, ActivityRequestDTO requestDTO) {
        Activity existingActivity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("attività non trovata con id: " + id));

        modelMapper.map(requestDTO, existingActivity);

        Activity updatedActivity = activityRepository.save(existingActivity);

        return modelMapper.map(updatedActivity, ActivityResponseDTO.class);
    }

    @Override
    @Transactional
    public void deleteActivity(UUID id) {
        if (!activityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Attività non trovata con id: " + id);
        }
        activityRepository.deleteById(id);
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



