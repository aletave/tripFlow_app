package com.tripflow.booking.data.dto.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrenotazioneAttivitaRequest {

    @NotNull(message = "id attività obbligatorio")
    private UUID attivitaId;
}