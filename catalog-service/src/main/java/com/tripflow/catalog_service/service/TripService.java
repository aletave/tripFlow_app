package com.tripflow.catalog_service.service;

import com.tripflow.catalog_service.dto.request.TripRequestDTO;
import com.tripflow.catalog_service.dto.response.TripResponseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TripService {

    TripResponseDTO createTrip(TripRequestDTO requestDTO, UUID organizerId);

    TripResponseDTO getTripById(UUID id);

    TripResponseDTO updateTrip(UUID id, TripRequestDTO requestDTO, UUID organizerId);

    void deleteTrip(UUID id, UUID organizerId);

    List<TripResponseDTO> getTripsByOrganizer(UUID organizerId);

    List<TripResponseDTO> searchTrips(String destination, LocalDate startDate, LocalDate endDate,
                                      BigDecimal minPrice, BigDecimal maxPrice, Integer minAvailableSpots);

    void decreaseAvailableSpots(UUID tripId, int spots);

    void increaseAvailableSpots(UUID tripId, int spots);

}