package com.tripflow.booking.data.dto.responses;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrenotazioneAttivitaResponse {

    private UUID id;
    private UUID attivitaId;

    // snapshot
    private String nome;
    private BigDecimal prezzo;
    private Integer durataMinuti;

    private LocalDateTime aggiuntoIl;
}