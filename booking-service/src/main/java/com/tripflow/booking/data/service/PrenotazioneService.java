package com.tripflow.booking.data.service;

import com.tripflow.booking.data.dto.requests.PrenotazioneAttivitaRequest;
import com.tripflow.booking.data.dto.requests.PrenotazioneRequest;
import com.tripflow.booking.data.dto.responses.PrenotazioneResponse;
import com.tripflow.booking.data.entities.enums.StatoPrenotazione;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


public interface PrenotazioneService {

    PrenotazioneResponse creaPrenotazione(UUID viaggiatoreId, PrenotazioneRequest request);

    PrenotazioneResponse trovaPrenotazione(UUID prenotazioneId, UUID viaggiatoreId);

    List<PrenotazioneResponse> trovaMiePrenotazioni(UUID viaggiatoreId);

    List<PrenotazioneResponse> trovaMiePrenotazioniAttive(UUID viaggiatoreId);

    PrenotazioneResponse annullaPrenotazione(UUID prenotazioneId, UUID viaggiatoreId);

    PrenotazioneResponse aggiungiAttivita(UUID prenotazioneId,
                                          UUID viaggiatoreId,
                                          PrenotazioneAttivitaRequest request);

     //Rimuove un'attività da una prenotazione esistente.
     // Ricalcola il prezzo totale.
     //stesse regole di aggiungiAttivita.
    PrenotazioneResponse rimuoviAttivita(UUID prenotazioneId,
                                         UUID viaggiatoreId,
                                         UUID attivitaId);



    // per l'organizzatore
    //Tutte le prenotazioni ricevute per un viaggio.
    //Pensata per l'organizzatore che vuole vedere i partecipanti al suo viaggio.
    List<PrenotazioneResponse> trovaPrenotazioniPerViaggio(UUID viaggioId, UUID organizzatoreId);


    //Ricerca dinamica con filtri opzionali(specification).

    List<PrenotazioneResponse> ricerca(UUID viaggiatoreId,
                                       UUID viaggioId,
                                       StatoPrenotazione stato,
                                       LocalDateTime da,
                                       LocalDateTime a,
                                       BigDecimal prezzoMin,
                                       BigDecimal prezzoMax);



    PrenotazioneResponse confermaPrenotazione(UUID prenotazioneId);

    int completaPrenotazioniScadute();
}