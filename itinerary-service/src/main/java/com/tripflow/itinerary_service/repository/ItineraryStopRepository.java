package com.tripflow.itinerary_service.repository;

import com.tripflow.itinerary_service.model.ItineraryStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItineraryStopRepository extends JpaRepository<ItineraryStop, Long> {

    // tutte le tappe di un itinerario ordinate
    List<ItineraryStop> findByItineraryIdOrderByStopOrderAsc(Long itineraryId);

    // elimina tutte le tappe di un itinerario
    void deleteByItineraryId(Long itineraryId);
}