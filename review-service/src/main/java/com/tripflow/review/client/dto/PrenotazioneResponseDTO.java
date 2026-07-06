package com.tripflow.review.client.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;


@Data
public class PrenotazioneResponseDTO {

    private UUID id;
    private UUID viaggioId;
    private StatoPrenotazione stato;
    private List<PrenotazioneAttivitaResponseDTO> attivitaSelezionate;
}