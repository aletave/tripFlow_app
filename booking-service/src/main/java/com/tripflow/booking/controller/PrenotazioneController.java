package com.tripflow.booking.controller;

import com.tripflow.booking.configSecurity.SecurityUtils;
import com.tripflow.booking.configSecurity.UtenteAutenticato;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrenotazioneResponse crea(@Valid @RequestBody PrenotazioneRequest request) {
        return prenotazioneService.creaPrenotazione(SecurityUtils.viaggiatoreId(), request);
    }

    @GetMapping("/{id}")
    public PrenotazioneResponse trova(@PathVariable UUID id) {
        return prenotazioneService.trovaPrenotazione(id, SecurityUtils.viaggiatoreId());
    }

    @GetMapping("/mie")
    public List<PrenotazioneResponse> mie() {
        return prenotazioneService.trovaMiePrenotazioni(SecurityUtils.viaggiatoreId());
    }

    @GetMapping("/mie/attive")
    public List<PrenotazioneResponse> mieAttive() {
        return prenotazioneService.trovaMiePrenotazioniAttive(SecurityUtils.viaggiatoreId());
    }

    //Transizione di stato: la prenotazione non viene cancellata
    @PatchMapping("/{id}/annulla")
    public PrenotazioneResponse annulla(@PathVariable UUID id) {
        return prenotazioneService.annullaPrenotazione(id, SecurityUtils.viaggiatoreId());
    }


    // Gestione attività (su prenotazione esistente)

    @PostMapping("/{id}/attivita")
    @ResponseStatus(HttpStatus.CREATED)
    public PrenotazioneResponse aggiungiAttivita(
            @PathVariable UUID id,
            @Valid @RequestBody PrenotazioneAttivitaRequest request) {
        return prenotazioneService.aggiungiAttivita(id, SecurityUtils.viaggiatoreId(), request);
    }

    @DeleteMapping("/{id}/attivita/{attivitaId}")
    public PrenotazioneResponse rimuoviAttivita(
            @PathVariable UUID id,
            @PathVariable UUID attivitaId) {
        return prenotazioneService.rimuoviAttivita(id, SecurityUtils.viaggiatoreId(), attivitaId);
    }


    // Organizzatore
    //Tutte le prenotazioni ricevute per un viaggio.

    //Solo un ORGANIZER può chiamarlo. che il viaggio sia proprio suo
    //lo verifica il service interrogando il catalog
    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/viaggio/{viaggioId}")
    public List<PrenotazioneResponse> perViaggio(@PathVariable UUID viaggioId) {
        return prenotazioneService.trovaPrenotazioniPerViaggio(
                viaggioId, SecurityUtils.viaggiatoreId());
    }

    //ricerca dinamica

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


        UtenteAutenticato utente = SecurityUtils.utenteCorrente();
        if (!"ORGANIZER".equals(utente.ruolo())) {
            viaggiatoreId = utente.id();
        }

        return prenotazioneService.ricerca(viaggiatoreId, viaggioId, stato, da, a, prezzoMin, prezzoMax);
    }
}