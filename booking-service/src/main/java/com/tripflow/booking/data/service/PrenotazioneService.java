package com.tripflow.booking.data.service;

import com.tripflow.booking.data.dto.requests.PrenotazioneAttivitaRequest;
import com.tripflow.booking.data.dto.requests.PrenotazioneRequest;
import com.tripflow.booking.data.dto.responses.PrenotazioneResponse;
import com.tripflow.booking.data.entities.enums.StatoPrenotazione;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

//Service delle prenotazioni
//i metodi per il viaggiatore hanno viaggiatoreId come parametro
//Le transizioni di stato sono metodi dedicati
//eccezioni da gestire nel GEH

public interface PrenotazioneService {

    // viaggiatore

    //---------------------------

    //Crea una nuova prenotazione per il viaggiatore indicato.

    PrenotazioneResponse creaPrenotazione(UUID viaggiatoreId, PrenotazioneRequest request);

     // Restituisce una prenotazione del viaggiatore
     // PrenotazioneNotFoundException se l'id non esiste
     // AccessDeniedException         se la prenotazione non appartiene al viaggiatore

    PrenotazioneResponse trovaPrenotazione(UUID prenotazioneId, UUID viaggiatoreId);

    // Lista delle prenotazioni del viaggiatore, dalla più recente.
    List<PrenotazioneResponse> trovaMiePrenotazioni(UUID viaggiatoreId);

    //Lista delle prenotazioni attive del viaggiatore (IN_ATTESA + CONFERMATA).
    //Usato dalla schermata principale dell'app: viaggi che mi aspettano
    List<PrenotazioneResponse> trovaMiePrenotazioniAttive(UUID viaggiatoreId);


    // Annulla una prenotazione del viaggiatore.

    // Regole:
    // - solo il proprietario può annullare
    // - sono annullabili: IN_ATTESA, CONFERMATA
    // - non sono annullabili: ANNULLATA, COMPLETATA
    // - se la prenotazione era CONFERMATA (pagata), avvia il rimborso

    //PrenotazioneNotFoundException GEH
    //AccessDeniedException GEH
    //StatoPrenotazioneException se lo stato attuale non permette l'annullamento GEH
    PrenotazioneResponse annullaPrenotazione(UUID prenotazioneId, UUID viaggiatoreId);


    // Gestione attività (su prenotazione esistente)

     //Aggiunge un'attività a una prenotazione esistente.
     // Ricalcola il prezzo totale
     // Regole:
     // - solo il proprietario può modificare
     // - permesso solo se stato = IN_ATTESA
     //   (una volta CONFERMATA non si tocca, già pagata)

     //PrenotazioneNotFoundException
     //AccessDeniedException
     //StatoPrenotazioneException se lo stato non permette modifiche
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
    List<PrenotazioneResponse> trovaPrenotazioniPerViaggio(UUID viaggioId);


    //Ricerca dinamica con filtri opzionali(specification).
    //Tutti i parametri sono opzionali: passare null per non applicare il filtro.

    List<PrenotazioneResponse> ricerca(UUID viaggiatoreId,
                                       UUID viaggioId,
                                       StatoPrenotazione stato,
                                       LocalDateTime da,
                                       LocalDateTime a,
                                       BigDecimal prezzoMin,
                                       BigDecimal prezzoMax);



    //Transizioni di stato (chiamate da altri service o job)

    //------------------------------

    //Conferma una prenotazione: tipicamente chiamata dal PagamentoService
    //dopo che il pagamento è andato a buon fine.
    //PrenotazioneNotFoundException
    //StatoPrenotazioneException se la prenotazione non è in IN_ATTESA

    PrenotazioneResponse confermaPrenotazione(UUID prenotazioneId);


     //Sposta in COMPLETATA tutte le prenotazioni CONFERMATA con viaggio terminato.

     //ritorna numero di prenotazioni completate
    int completaPrenotazioniScadute();
}