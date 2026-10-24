package com.tripflow.booking.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "booking.sconto")
public record ScontoProperties(@NotBlank String codiceCompleanno) {
}
