package com.tripflow.catalog_service.repository;

import com.tripflow.catalog_service.model.Trip;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TripSpecification {
    public static Specification<Trip> getFilterSpecification(
            String destination, LocalDate startDate, LocalDate endDate,
            BigDecimal minPrice, BigDecimal maxPrice, Integer minAvailableSpots) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (destination != null && !destination.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("destination")),
                        "%" + destination.toLowerCase() + "%"));
            }

            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), endDate));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (minAvailableSpots != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("availableSpots"), minAvailableSpots));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

