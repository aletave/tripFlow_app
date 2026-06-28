package com.tripflow.catalog_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingMessageDTO {
    private UUID tripId;
    private int spots; //posti

}
