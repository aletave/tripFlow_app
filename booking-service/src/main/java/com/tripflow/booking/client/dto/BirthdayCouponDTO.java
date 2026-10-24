package com.tripflow.booking.client.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BirthdayCouponDTO {
    private boolean eligible;
    private String code;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String reason;
    private String message;
}
