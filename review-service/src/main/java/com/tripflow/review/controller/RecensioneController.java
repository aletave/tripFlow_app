package com.tripflow.review.controller;

import com.tripflow.review.configSecurity.SecurityUtils;
import com.tripflow.review.data.dto.requests.RecensioneRequest;
import com.tripflow.review.data.dto.requests.RecensioneUpdateRequest;
import com.tripflow.review.data.dto.responses.RecensioneResponse;
import com.tripflow.review.data.entities.enums.TipoOggetto;
import com.tripflow.review.data.service.RecensioneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/recensioni")
@RequiredArgsConstructor
@Slf4j
public class RecensioneController {

    private final RecensioneService recensioneService;

    //viaggiatore

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecensioneResponse creaRecensione(@Valid @RequestBody RecensioneRequest request) {
        return recensioneService.creaRecensione(
                SecurityUtils.viaggiatoreId(), SecurityUtils.nome(), request);
    }


    @GetMapping("/mie")
    public List<RecensioneResponse> trovaMieRecensioni() {
        return recensioneService.trovaMieRecensioni(SecurityUtils.viaggiatoreId());
    }


    @PutMapping("/{id}")
    public RecensioneResponse modificaRecensione(
            @PathVariable("id") UUID recensioneId,
            @Valid @RequestBody RecensioneUpdateRequest request) {

        return recensioneService.modificaRecensione(recensioneId, SecurityUtils.viaggiatoreId(), request);
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminaRecensione(@PathVariable("id") UUID recensioneId) {
        recensioneService.eliminaRecensione(recensioneId, SecurityUtils.viaggiatoreId());
    }


    @GetMapping("/puo-recensire")
    public boolean puoRecensire(
            @RequestParam("prenotazioneId") UUID prenotazioneId,
            @RequestParam("tipoOggetto") TipoOggetto tipoOggetto,
            @RequestParam("oggettoId") UUID oggettoId) {

        return recensioneService.puoRecensire(
                SecurityUtils.viaggiatoreId(), prenotazioneId, tipoOggetto, oggettoId);
    }


    @GetMapping("/{id}")
    public RecensioneResponse trovaRecensione(@PathVariable("id") UUID recensioneId) {
        return recensioneService.trovaRecensione(recensioneId);
    }


    @GetMapping("/oggetto/{oggettoId}")
    public List<RecensioneResponse> trovaRecensioniOggetto(
            @PathVariable("oggettoId") UUID oggettoId) {

        return recensioneService.trovaRecensioniOggetto(oggettoId);
    }


    @GetMapping("/oggetto/{oggettoId}/media")
    public Double mediaValutazione(@PathVariable("oggettoId") UUID oggettoId) {
        return recensioneService.mediaValutazione(oggettoId);
    }


    @GetMapping("/oggetto/{oggettoId}/conta")
    public long contaRecensioni(@PathVariable("oggettoId") UUID oggettoId) {
        return recensioneService.contaRecensioni(oggettoId);
    }
}