package com.tripflow.itinerary_service.dto.errors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceError {
    private Date timestamp;
    private String path;
    private String message;
}
