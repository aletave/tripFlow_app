package com.tripflow.review.client.dto;

import lombok.Data;

import java.util.UUID;

//mi serve solo l'id dell'attività per verificare che l'oggetto
//recensito sia effettivamente parte della prenotazione
@Data
public class PrenotazioneAttivitaResponseDTO {

    private UUID attivitaId;
}