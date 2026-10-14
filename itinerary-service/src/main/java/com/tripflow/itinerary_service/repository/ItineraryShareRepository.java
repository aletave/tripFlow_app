package com.tripflow.itinerary_service.repository;

import com.tripflow.itinerary_service.model.ItineraryShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItineraryShareRepository extends JpaRepository<ItineraryShare, Long> {

    Optional<ItineraryShare> findByItineraryIdAndSharedWithUserId(Long itineraryId, UUID sharedWithUserId);

    List<ItineraryShare> findByItineraryId(Long itineraryId);

    List<ItineraryShare> findBySharedWithUserId(UUID sharedWithUserId);

    boolean existsByItineraryIdAndSharedWithUserId(Long itineraryId, UUID sharedWithUserId);

    void deleteByItineraryIdAndSharedWithUserId(Long itineraryId, UUID sharedWithUserId);
}