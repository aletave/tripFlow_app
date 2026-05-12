package com.tripflow.booking.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

//Replica locale del DTO esposto da catalog-service.
@Data
public class TripResponseDTO {
    private UUID id;
    private String name;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal price;
    private Integer availableSpots;
    private UUID organizerId;
    private String description;
    private List<String> images;
    private List<ActivityResponseDTO> activities;
}