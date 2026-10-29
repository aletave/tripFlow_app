package com.tripflow.itinerary_service.repository;

import com.tripflow.itinerary_service.model.ItineraryShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accesso alle condivisioni degli itinerari.
 */
public interface ItineraryShareRepository extends JpaRepository<ItineraryShare, Long> {

    Optional<ItineraryShare> findByItineraryIdAndSharedWithUserId(Long itineraryId, UUID sharedWithUserId);

    List<ItineraryShare> findByItineraryId(Long itineraryId);

    List<ItineraryShare> findBySharedWithUserId(UUID sharedWithUserId);

    boolean existsByItineraryId(Long itineraryId);

    boolean existsByItineraryIdAndSharedWithUserId(Long itineraryId, UUID sharedWithUserId);

    /** Elimina tutte le condivisioni di un itinerario (quando torna PRIVATE). */
    @Modifying
    @Query("delete from ItineraryShare s where s.itineraryId = :itineraryId")
    void deleteByItineraryId(@Param("itineraryId") Long itineraryId);

    void deleteByItineraryIdAndSharedWithUserId(Long itineraryId, UUID sharedWithUserId);
}