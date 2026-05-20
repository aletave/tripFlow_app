package com.tripflow.review.client;

import com.tripflow.review.client.dto.ActivityResponseDTO;
import com.tripflow.review.client.dto.TripResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

//client REST verso catalog-service
@FeignClient(name = "catalog-service", url = "${tripflow.catalog.base-url}")
public interface CatalogClient {

    @GetMapping("/api/v1/trips/{id}")
    TripResponseDTO getTrip(@PathVariable("id") UUID id);

    @GetMapping("/api/v1/activities/{id}")
    ActivityResponseDTO getActivity(@PathVariable("id") UUID id);
}