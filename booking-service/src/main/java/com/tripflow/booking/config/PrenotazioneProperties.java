package com.tripflow.booking.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "booking.prenotazione")
public record PrenotazioneProperties(@NotNull Duration ttl, @NotNull Duration finestraPagamento) {
}
