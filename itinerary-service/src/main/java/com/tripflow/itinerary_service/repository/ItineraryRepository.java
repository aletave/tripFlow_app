package com.tripflow.itinerary_service.repository;

import com.tripflow.itinerary_service.model.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    // tutti gli itinerari di un owner
    List<Itinerary> findByOwnerId(UUID ownerId);

    // recupera un itinerario specifico dell'owner
    Optional<Itinerary> findByIdAndOwnerId(Long id, UUID ownerId);

    // verifica esistenza dell'itinerario per owner
    boolean existsByIdAndOwnerId(Long id, UUID ownerId);

    // opzionale: filtra per visibilità
    List<Itinerary> findByVisibility(String visibility);
}
