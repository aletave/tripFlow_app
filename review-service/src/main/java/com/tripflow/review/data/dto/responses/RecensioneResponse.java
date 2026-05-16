package com.tripflow.review.data.dto.responses;

import com.tripflow.review.data.entities.enums.TipoOggetto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecensioneResponse {

    private UUID id;

    private UUID viaggiatoreId;
    private UUID prenotazioneId;

    private TipoOggetto tipoOggetto;
    private UUID oggettoId;


    //Dati SNAPSHOT
    private String oggettoNome;

    private String autoreNome;


    //contenuto recensione
    private Short valutazione;
    private String titolo;
    private String commento;


    // AUDIT
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
