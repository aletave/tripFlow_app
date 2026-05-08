package com.tripflow.catalog_service.service;

import com.tripflow.catalog_service.dto.request.TripRequestDTO;
import com.tripflow.catalog_service.dto.response.TripResponseDTO;
import com.tripflow.catalog_service.exception.ResourceNotFoundException;
import com.tripflow.catalog_service.model.Trip;
import com.tripflow.catalog_service.repository.TripRepository;
import com.tripflow.catalog_service.repository.TripSpecification;
import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class TripServiceImpl implements TripService {
    private final ModelMapper modelMapper;
    private final TripRepository tripRepository;

    public TripServiceImpl(ModelMapper modelMapper, TripRepository tripRepository) {
        this.modelMapper = modelMapper;
        this.tripRepository = tripRepository;
    }

    @Override
    @Transactional
    public TripResponseDTO createTrip(TripRequestDTO requestDTO, UUID organizerId) {
        Trip trip = modelMapper.map(requestDTO, Trip.class);

        trip.setOrganizerId(organizerId);

        Trip savedTrip = tripRepository.save(trip);

        //fare chiamata con rabbitmq appena capisco come si usa

        return modelMapper.map(savedTrip, TripResponseDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponseDTO getTripById(UUID id) {
        Trip trip = tripRepository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("viaggio non trovato con id: " + id));

        return modelMapper.map(trip, TripResponseDTO.class);
    }

    @Override
    @Transactional
    public TripResponseDTO updateTrip(UUID id, TripRequestDTO requestDTO, UUID organizerId) {
        Trip existingTrip = tripRepository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("viaggio non trovato con id: " + id));

        if (!existingTrip.getOrganizerId().equals(organizerId)) {
            throw new RuntimeException("Non hai i permessi per modificare questo viaggio");
        }

        modelMapper.map(requestDTO, existingTrip);

        Trip updatedTrip = tripRepository.save(existingTrip);

        //sempre rabbitmq

        return modelMapper.map(updatedTrip, TripResponseDTO.class);
    }

    @Override
    @Transactional
    public void deleteTrip(UUID id, UUID organizerId) {
        Trip trip = tripRepository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("viaggio non trovato con questo id : " + id));

        if (!trip.getOrganizerId().equals(organizerId)) {
            throw new RuntimeException("Non hai i permessi per eliminanre questo viaggio");
        }

        tripRepository.delete(trip);

        //sempre rabbitmq
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponseDTO> getTripsByOrganizer(UUID organizerId) {
        List<Trip> trips = tripRepository.findByOrganizerId(organizerId);

        return trips.stream().map(trip -> modelMapper.map(trip, TripResponseDTO.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponseDTO> searchTrips(String destination, LocalDate startDate, LocalDate endDate, BigDecimal minPrice, BigDecimal maxPrice, Integer minAvailableSpots) {

        Specification<Trip> spec = TripSpecification.getFilterSpecification(
                destination, startDate, endDate, minPrice, maxPrice, minAvailableSpots);

        List<Trip> trips = tripRepository.findAll(spec);

        return trips.stream()
                .map(trip -> modelMapper.map(trip, TripResponseDTO.class))
                .toList();
    }
}







