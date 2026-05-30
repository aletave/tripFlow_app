package com.tripflow.review.mapper;

import com.tripflow.review.data.dto.responses.RecensioneResponse;
import com.tripflow.review.data.entities.Recensione;

//mapper

//Conversione Entity -> DTO per Recensione.
public final class RecensioneMapper {

    private RecensioneMapper() {
    }

    public static RecensioneResponse toResponse(Recensione r) {
        if (r == null) return null;
        return RecensioneResponse.builder()
                .id(r.getId())
                .viaggiatoreId(r.getViaggiatoreId())
                .prenotazioneId(r.getPrenotazioneId())
                .tipoOggetto(r.getTipoOggetto())
                .oggettoId(r.getOggettoId())
                .oggettoNome(r.getOggettoNomeSnap())
                .autoreNome(r.getAutoreNomeSnap())
                .valutazione(r.getValutazione())
                .titolo(r.getTitolo())
                .commento(r.getCommento())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}