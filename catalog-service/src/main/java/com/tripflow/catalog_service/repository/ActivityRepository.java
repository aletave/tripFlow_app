package com.tripflow.catalog_service.repository;

import com.tripflow.catalog_service.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.*;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<Activity,UUID>, JpaSpecificationExecutor<Activity> {

    List<Activity> findByTripId(UUID tripId);

}
