package com.tripflow.booking.controller;

import com.tripflow.booking.data.dto.requests.PrenotazioneAttivitaRequest;
import com.tripflow.booking.data.dto.requests.PrenotazioneRequest;
import com.tripflow.booking.data.dto.responses.PrenotazioneResponse;
import com.tripflow.booking.data.entities.enums.StatoPrenotazione;
import com.tripflow.booking.data.service.PrenotazioneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prenotazioni")
@RequiredArgsConstructor
@Slf4j
public class PrenotazioneController {

    private final PrenotazioneService prenotazioneService;

    //Header con l'id del viaggiatore. TODO: sostituire con JWT (Spring Security).
    private static final String HEADER_VIAGGIATORE = "X-Viaggiatore-Id";

    //viaggiatore
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrenotazioneResponse crea(
            @RequestHeader(HEADER_VIAGGIATORE) UUID viaggiatoreId,
            @Valid @RequestBody PrenotazioneRequest request) {
        return prenotazioneService.creaPrenotazione(viaggiatoreId, request);
    }

    @GetMapping("/{id}")
    public PrenotazioneResponse trova(
            @PathVariable UUID id,
            @RequestHeader(HEADER_VIAGGIATORE) UUID viaggiatoreId) {
        return prenotazioneService.trovaPrenotazione(id, viaggiatoreId);
    }

    @GetMapping("/mie")
    public List<PrenotazioneResponse> mie(
            @RequestHeader(HEADER_VIAGGIATORE) UUID viaggiatoreId) {
        return prenotazioneService.trovaMiePrenotazioni(viaggiatoreId);
    }

    @GetMapping("/mie/attive")
    public List<PrenotazioneResponse> mieAttive(
            @RequestHeader(HEADER_VIAGGIATORE) UUID viaggiatoreId) {
        return prenotazioneService.trovaMiePrenotazioniAttive(viaggiatoreId);
    }

    //Transizione di stato: la prenotazione non viene cancellata.
    @PatchMapping("/{id}/annulla")
    public PrenotazioneResponse annulla(
            @PathVariable UUID id,
            @RequestHeader(HEADER_VIAGGIATORE) UUID viaggiatoreId) {
        return prenotazioneService.annullaPrenotazione(id, viaggiatoreId);
    }


    // Gestione attività (su prenotazione esistente)

    @PostMapping("/{id}/attivita")
    @ResponseStatus(HttpStatus.CREATED)
    public PrenotazioneResponse aggiungiAttivita(
            @PathVariable UUID id,
            @RequestHeader(HEADER_VIAGGIATORE) UUID viaggiatoreId,
            @Valid @RequestBody PrenotazioneAttivitaRequest request) {
        return prenotazioneService.aggiungiAttivita(id, viaggiatoreId, request);
    }

    @DeleteMapping("/{id}/attivita/{attivitaId}")
    public PrenotazioneResponse rimuoviAttivita(
            @PathVariable UUID id,
            @PathVariable UUID attivitaId,
            @RequestHeader(HEADER_VIAGGIATORE) UUID viaggiatoreId) {
        return prenotazioneService.rimuoviAttivita(id, viaggiatoreId, attivitaId);
    }


    // Organizzatore
    //Tutte le prenotazioni ricevute per un viaggio.


     //TODO: proteggere con Spring Security — solo l'organizzatore proprietario
     //del viaggio deve poter chiamare questo endpoint.

    @GetMapping("/viaggio/{viaggioId}")
    public List<PrenotazioneResponse> perViaggio(@PathVariable UUID viaggioId) {
        return prenotazioneService.trovaPrenotazioniPerViaggio(viaggioId);
    }

    //ricerca dinamica

    //TODO: limitare in base ai ruoli quando arriva Spring Security
    //(un viaggiatore non deve poter cercare le prenotazioni di altri).
    @GetMapping("/cerca")
    public List<PrenotazioneResponse> cerca(
            @RequestParam(required = false) UUID viaggiatoreId,
            @RequestParam(required = false) UUID viaggioId,
            @RequestParam(required = false) StatoPrenotazione stato,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime da,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime a,
            @RequestParam(required = false) BigDecimal prezzoMin,
            @RequestParam(required = false) BigDecimal prezzoMax) {
        return prenotazioneService.ricerca(viaggiatoreId, viaggioId, stato, da, a, prezzoMin, prezzoMax);
    }
}