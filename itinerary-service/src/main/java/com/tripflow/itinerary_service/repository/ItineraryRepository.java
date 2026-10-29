package com.tripflow.itinerary_service.repository;

import com.tripflow.itinerary_service.model.Itinerary;
import com.tripflow.itinerary_service.model.Visibility;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accesso agli itinerari. Le query caricano anche le tappe in un'unica lettura
 * ({@code @EntityGraph}), evitando una query per ogni itinerario.
 */
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    @Override
    @EntityGraph(attributePaths = "stops")
    Optional<Itinerary> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "stops")
    List<Itinerary> findAllById(Iterable<Long> ids);

    /** Itinerari di cui l'utente e' proprietario. */
    @EntityGraph(attributePaths = "stops")
    List<Itinerary> findByOwnerId(UUID ownerId);

    /** Itinerario con quel token, solo se ha ancora la visibilita' indicata. */
    @EntityGraph(attributePaths = "stops")
    Optional<Itinerary> findByPublicTokenAndVisibility(UUID publicToken, Visibility visibility);
}
