package com.tripflow.itinerary_service.repository;

import com.tripflow.itinerary_service.model.Itinerary;
import com.tripflow.itinerary_service.model.Visibility;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    @Override
    @EntityGraph(attributePaths = "stops")
    Optional<Itinerary> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "stops")
    List<Itinerary> findAllById(Iterable<Long> ids);

    // tutti gli itinerari di un owner
    @EntityGraph(attributePaths = "stops")
    List<Itinerary> findByOwnerId(UUID ownerId);

    @EntityGraph(attributePaths = "stops")
    Optional<Itinerary> findByPublicTokenAndVisibility(UUID publicToken, Visibility visibility);

    // recupera un itinerario specifico dell'owner
    @EntityGraph(attributePaths = "stops")
    Optional<Itinerary> findByIdAndOwnerId(Long id, UUID ownerId);

    // verifica esistenza dell'itinerario per owner
    boolean existsByIdAndOwnerId(Long id, UUID ownerId);

    // opzionale: filtra per visibilità
    @EntityGraph(attributePaths = "stops")
    List<Itinerary> findByVisibility(Visibility visibility);
}
