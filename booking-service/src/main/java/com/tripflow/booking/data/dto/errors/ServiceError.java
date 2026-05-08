package com.tripflow.booking.data.dto.errors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceError {
    private Date timestamp;
    private String path;
    private String message;
}
