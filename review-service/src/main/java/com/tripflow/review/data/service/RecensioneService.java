package com.tripflow.review.data.service;

import com.tripflow.review.data.dto.requests.RecensioneRequest;
import com.tripflow.review.data.dto.requests.RecensioneUpdateRequest;
import com.tripflow.review.data.dto.responses.RecensioneResponse;
import com.tripflow.review.data.entities.enums.TipoOggetto;

import java.util.List;
import java.util.UUID;


public interface RecensioneService {


    RecensioneResponse creaRecensione(UUID viaggiatoreId,
                                      String autoreNome,
                                      RecensioneRequest request);


    RecensioneResponse trovaRecensione(UUID recensioneId);


    //lista recensioni viaggiatore (dalla più recente)
    List<RecensioneResponse> trovaMieRecensioni(UUID viaggiatoreId);


    RecensioneResponse modificaRecensione(UUID recensioneId,
                                          UUID viaggiatoreId,
                                          RecensioneUpdateRequest request);


    void eliminaRecensione(UUID recensioneId, UUID viaggiatoreId);


    //pubblici

    List<RecensioneResponse> trovaRecensioniOggetto(UUID oggettoId);

    Double mediaValutazione(UUID oggettoId);

    long contaRecensioni(UUID oggettoId);

    boolean puoRecensire(UUID viaggiatoreId, UUID prenotazioneId, TipoOggetto tipoOggetto, UUID oggettoId);

}