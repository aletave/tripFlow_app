package com.tripflow.catalog_service.service;

import com.tripflow.catalog_service.dto.request.ActivityRequestDTO;
import com.tripflow.catalog_service.dto.response.ActivityResponseDTO;
import com.tripflow.catalog_service.exception.ResourceNotFoundException;
import com.tripflow.catalog_service.mapper.ActivityMapper;
import com.tripflow.catalog_service.model.Activity;
import com.tripflow.catalog_service.model.Trip;
import com.tripflow.catalog_service.repository.ActivityRepository;
import com.tripflow.catalog_service.repository.TripRepository;
import com.tripflow.catalog_service.repository.ActivitySpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final TripRepository tripRepository;
    private final ActivityMapper activityMapper;

    public ActivityServiceImpl(ActivityRepository activityRepository,
                               TripRepository tripRepository,
                               ActivityMapper activityMapper) {
        this.activityMapper=activityMapper;
        this.activityRepository=activityRepository;
        this.tripRepository=tripRepository;
    }

    @Override
    @Transactional
    public ActivityResponseDTO createActivity(ActivityRequestDTO requestDTO) {
        Trip trip = tripRepository.findById(requestDTO.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("viaggio non trovato con id: " + requestDTO.getTripId()));

        Activity activity = activityMapper.toEntity(requestDTO);

        activity.setTrip(trip);

        Activity savedActivity = activityRepository.save(activity);

        return activityMapper.toResponseDTO(savedActivity);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityResponseDTO getActivityById(UUID id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("attività non trovata con id: " + id));

        return activityMapper.toResponseDTO(activity);
    }

    @Override
    @Transactional
    public ActivityResponseDTO updateActivity(UUID id, ActivityRequestDTO requestDTO) {
        Activity existingActivity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("attività non trovata con id: " + id));

        existingActivity.setName(requestDTO.getName());
        existingActivity.setDescription(requestDTO.getDescription());
        existingActivity.setDuration(requestDTO.getDuration());
        existingActivity.setPrice(requestDTO.getPrice());
        existingActivity.setAvailableSpots(requestDTO.getAvailableSpots());

        Activity updatedActivity = activityRepository.save(existingActivity);

        return activityMapper.toResponseDTO(updatedActivity);
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
                .map(activityMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponseDTO> searchActivities(String name, String description, Integer duration,
                                                      BigDecimal minPrice, BigDecimal maxPrice, Integer minAvailableSpots) {

        Specification<Activity> spec = ActivitySpecification.getFilterSpecification(
                name, description, duration, minPrice, maxPrice, minAvailableSpots);

        List<Activity> activities = activityRepository.findAll(spec);

        return activities.stream()
                .map(activityMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
}



