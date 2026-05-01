package com.tripflow.catalog_service.repository;

import com.tripflow.catalog_service.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<Activity,UUID> {

    List<Activity> findByTripId(UUID tripId);

    @Query
    ("SELECT a FROM Activity a WHERE " +
    "(:name IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
    "(:description IS NULL OR LOWER(a.description) LIKE LOWER(CONCAT('%', :description, '%'))) AND " +
    "(:duration IS NULL OR a.duration = :duration) AND " +
    "(:minPrice IS NULL OR a.price >= :minPrice) AND " +
    "(:maxPrice IS NULL OR a.price <= :maxPrice) AND " +
    "(:minAvailableSpots IS NULL OR a.availableSpots >= :minAvailableSpots)")

    List<Activity> searchActivities(
            @Param("name") String name, @Param("description") String description,
            @Param("duration") Integer duration, @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice, @Param("minAvailableSpots") Integer minAvailableSpots);



}
