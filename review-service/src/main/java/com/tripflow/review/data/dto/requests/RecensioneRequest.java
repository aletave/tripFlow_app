package com.tripflow.review.data.dto.requests;

import com.tripflow.review.data.entities.enums.TipoOggetto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class RecensioneRequest {

    @NotNull(message = "id prenotazione obbligatorio")
    private UUID prenotazioneId;

    @NotNull(message = "L'id dell'oggetto è obbligatorio")
    private UUID oggettoId;

    @NotNull(message = "il tipo oggetto è obbligatorio (viaggio o attivita")
    private TipoOggetto tipoOggetto; //viaggio o attività

    @NotNull(message = "valutazione obbligatoria")
    @Min(value = 1, message = "valutazione minima 1")
    @Max(value = 5, message = "valutazione massima 5")
    private Short valutazione;

    @Size(max = 200, message = "il titolo non può superare i 200 caratteri")
    private String titolo;

    @Size(max = 5000, message = "il commento non può superare i 5000 caratteri")
    private String commento;
}
