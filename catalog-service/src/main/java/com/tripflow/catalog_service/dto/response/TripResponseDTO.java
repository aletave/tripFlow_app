package com.tripflow.catalog_service.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
