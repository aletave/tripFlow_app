package com.tripflow.catalog_service.repository;

import com.tripflow.catalog_service.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByOrganizerId(UUID organizerId);

    @Query
    ("SELECT t FROM Trip t WHERE " +
    "(:destination IS NULL OR LOWER(t.destination) LIKE LOWER(CONCAT('%', :destination, '%'))) AND " +
    "(:startDate IS NULL OR t.startDate >= :startDate) AND "+
    "(:endDate IS NULL OR t.endDate <= :endDate) AND "+
    "(:minPrice IS NULL OR t.price >= :minPrice) AND " +
    "(:maxPrice IS NULL OR t.price <= :maxPrice) AND " +
    "(:minAvailableSpots IS NULL OR t.availableSpots >= :minAvailableSpots)")

    List<Trip> searchTrips(
            @Param("destination") String destination, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice, @Param("minAvailableSpots") Integer minAvailableSpots);

}
