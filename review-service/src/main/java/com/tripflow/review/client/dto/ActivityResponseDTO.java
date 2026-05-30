package com.tripflow.review.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

//replica del dto di catalog
@Data
public class ActivityResponseDTO {
    private UUID id;
    private String name;
    private String description;
    private Integer duration;
    private BigDecimal price;
    private Integer availableSpots;
    private UUID tripId;
}