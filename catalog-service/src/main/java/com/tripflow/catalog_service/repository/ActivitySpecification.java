package com.tripflow.catalog_service.repository;

import com.tripflow.catalog_service.model.Activity;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ActivitySpecification {

    public static Specification<Activity> getFilterSpecification(
            String name, String description, Integer duration,
            BigDecimal minPrice, BigDecimal maxPrice, Integer minAvailableSpots) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),
                        "%" + name.toLowerCase() + "%"
                ));
            }

            if (description != null && !description.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")),
                        "%" + description.toLowerCase() + "%"
                ));
            }

            if (duration != null) {
                predicates.add(criteriaBuilder.equal(root.get("duration"), duration));
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