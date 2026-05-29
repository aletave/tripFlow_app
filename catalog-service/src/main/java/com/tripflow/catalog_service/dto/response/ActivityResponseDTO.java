package com.tripflow.catalog_service.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

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
