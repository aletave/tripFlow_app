package com.tripflow.review.client.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Replica SLIM del DTO esposto da booking-service.
 *
 * Contiene solo i campi che servono al review per validare la
 * recensibilità: tutti gli altri campi del JSON arrivano ma vengono
 * ignorati da Jackson. Se in futuro ne servirà uno in più, lo
 * aggiungiamo qui senza toccare il booking.
 */
@Data
public class PrenotazioneResponseDTO {

    private UUID id;
    private UUID viaggioId;
    private StatoPrenotazione stato;
    private List<PrenotazioneAttivitaResponseDTO> attivitaSelezionate;
}