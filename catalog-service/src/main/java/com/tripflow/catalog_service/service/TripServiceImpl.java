package com.tripflow.catalog_service.service;

import com.tripflow.catalog_service.dto.request.TripRequestDTO;
import com.tripflow.catalog_service.dto.response.TripResponseDTO;
import com.tripflow.catalog_service.exception.ResourceNotFoundException;
import com.tripflow.catalog_service.mapper.TripMapper;
import com.tripflow.catalog_service.model.Trip;
import com.tripflow.catalog_service.repository.TripRepository;
import com.tripflow.catalog_service.repository.TripSpecification;
import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class TripServiceImpl implements TripService {
    private final ModelMapper modelMapper;
    private final TripMapper tripMapper;
    private final TripRepository tripRepository;
    private final TripEventProducer tripEventProducer;

    public TripServiceImpl(ModelMapper modelMapper, TripMapper tripMapper,
            TripRepository tripRepository, TripEventProducer tripEventProducer) {
        this.modelMapper = modelMapper;
        this.tripMapper = tripMapper;
        this.tripRepository = tripRepository;
        this.tripEventProducer = tripEventProducer;
    }

    @Override
    @Transactional
    public TripResponseDTO createTrip(TripRequestDTO requestDTO, UUID organizerId) {
        Trip trip = modelMapper.map(requestDTO, Trip.class);
        trip.setOrganizerId(organizerId);

        Trip savedTrip = tripRepository.save(trip);

        TripResponseDTO responseDTO = tripMapper.toDTO(savedTrip);
        tripEventProducer.sendTripCreatedEvent(responseDTO);

        return responseDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponseDTO getTripById(UUID id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("viaggio non trovato con id: " + id));

        return tripMapper.toDTO(trip);
    }

    @Override
    @Transactional
    public TripResponseDTO updateTrip(UUID id, TripRequestDTO requestDTO, UUID organizerId) {
        Trip existingTrip = tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("viaggio non trovato con id: " + id));

        if (!existingTrip.getOrganizerId().equals(organizerId)) {
            throw new AccessDeniedException("Non hai i permessi per modificare questo viaggio");
        }

        modelMapper.map(requestDTO, existingTrip);

        Trip updatedTrip = tripRepository.save(existingTrip);

        TripResponseDTO responseDTO = tripMapper.toDTO(updatedTrip);
        tripEventProducer.sendTripUpdatedEvent(responseDTO);

        return responseDTO;
    }

    @Override
    @Transactional
    public void deleteTrip(UUID id, UUID organizerId) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("viaggio non trovato con questo id : " + id));

        if (!trip.getOrganizerId().equals(organizerId)) {
            throw new AccessDeniedException("Non hai i permessi per eliminare questo viaggio");
        }

        tripRepository.delete(trip);
        tripEventProducer.sendTripDeletedEvent(tripMapper.toDTO(trip));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponseDTO> getTripsByOrganizer(UUID organizerId) {
        return tripRepository.findByOrganizerId(organizerId)
                .stream()
                .map(tripMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponseDTO> getAllTrips() {
        return tripRepository.findAll()
                .stream()
                .map(tripMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponseDTO> searchTrips(String destination, LocalDate startDate, LocalDate endDate,
            BigDecimal minPrice, BigDecimal maxPrice, Integer minAvailableSpots) {

        Specification<Trip> spec = TripSpecification.getFilterSpecification(
                destination, startDate, endDate, minPrice, maxPrice, minAvailableSpots);

        return tripRepository.findAll(spec)
                .stream()
                .map(tripMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public void decreaseAvailableSpots(UUID tripId, int spots) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Viaggio non trovato con id " + tripId));

        int currentBooked = trip.getBookedSpots() != null ? trip.getBookedSpots() : 0;
        trip.setBookedSpots(currentBooked + spots);
        tripRepository.save(trip);

        System.out.println(
                "[CATALOG DB] Registrati " + spots + " nuovi posti prenotati per il viaggio " + trip.getName());
    }

    @Override
    @Transactional
    public void increaseAvailableSpots(UUID tripId, int spots) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Viaggio non trovato con id " + tripId));

        int currentBooked = trip.getBookedSpots() != null ? trip.getBookedSpots() : 0;
        trip.setBookedSpots(Math.max(0, currentBooked - spots));
        tripRepository.save(trip);

        System.out.println("[CATALOG DB] Rilasciati " + spots + " posti dal viaggio " + trip.getName());
    }
}
