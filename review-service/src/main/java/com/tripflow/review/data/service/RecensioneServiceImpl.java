package com.tripflow.review.data.service;

import com.tripflow.review.client.BookingClient;
import com.tripflow.review.client.CatalogClient;
import com.tripflow.review.client.dto.PrenotazioneResponseDTO;
import com.tripflow.review.client.dto.StatoPrenotazione;
import com.tripflow.review.data.dao.RecensioneRepository;
import com.tripflow.review.data.dto.requests.RecensioneRequest;
import com.tripflow.review.data.dto.requests.RecensioneUpdateRequest;
import com.tripflow.review.data.dto.responses.RecensioneResponse;
import com.tripflow.review.data.entities.Recensione;
import com.tripflow.review.data.entities.enums.TipoOggetto;
import com.tripflow.review.exception.RecensioneEsistenteException;
import com.tripflow.review.exception.RecensioneNotFoundException;
import com.tripflow.review.exception.ReviewException;
import com.tripflow.review.exception.StatoRecensioneException;
import com.tripflow.review.mapper.RecensioneMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RecensioneServiceImpl implements RecensioneService {

    private final RecensioneRepository recensioneRepository;
    private final BookingClient bookingClient;
    private final CatalogClient catalogClient;

    //Finestra temporale entro cui una recensione resta modificabile.
    //Configurabile via application.properties (default 30 giorni).
    @Value("${tripflow.review.modifica-entro-giorni:30}")
    private long modificaEntroGiorni;


    @Override
    public RecensioneResponse creaRecensione(UUID viaggiatoreId,
                                             String autoreNome,
                                             RecensioneRequest request) {

        log.info("Creazione recensione: viaggiatore={}, prenotazione={}, oggetto={}, tipo={}",
                viaggiatoreId, request.getPrenotazioneId(),
                request.getOggettoId(), request.getTipoOggetto());


        verificaRecensibilita(viaggiatoreId,
                request.getPrenotazioneId(),
                request.getTipoOggetto(),
                request.getOggettoId());


        if (recensioneRepository.findByPrenotazioneId(request.getPrenotazioneId()).isPresent()) {
            throw new RecensioneEsistenteException(
                    "Esiste già una recensione per la prenotazione " + request.getPrenotazioneId());
        }
        if (recensioneRepository
                .findByViaggiatoreIdAndOggettoId(viaggiatoreId, request.getOggettoId())
                .isPresent()) {
            throw new RecensioneEsistenteException(
                    "Hai già recensito questo "
                            + (request.getTipoOggetto() == TipoOggetto.VIAGGIO ? "viaggio" : "attività"));
        }


        String oggettoNomeSnap = recuperaNomeOggetto(request.getTipoOggetto(), request.getOggettoId());


        Recensione recensione = Recensione.builder()
                .viaggiatoreId(viaggiatoreId)
                .prenotazioneId(request.getPrenotazioneId())
                .tipoOggetto(request.getTipoOggetto())
                .oggettoId(request.getOggettoId())
                .oggettoNomeSnap(oggettoNomeSnap)
                .autoreNomeSnap(autoreNome)
                .valutazione(request.getValutazione())
                .titolo(request.getTitolo())
                .commento(request.getCommento())
                .build();

        Recensione saved = recensioneRepository.save(recensione);

        log.info("Recensione creata: id={}, oggetto={}, valutazione={}",
                saved.getId(), saved.getOggettoId(), saved.getValutazione());

        return RecensioneMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RecensioneResponse trovaRecensione(UUID recensioneId) {
        Recensione recensione = recensioneRepository.findById(recensioneId)
                .orElseThrow(() -> new RecensioneNotFoundException(recensioneId));
        return RecensioneMapper.toResponse(recensione);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecensioneResponse> trovaMieRecensioni(UUID viaggiatoreId) {
        return recensioneRepository
                .findByViaggiatoreIdOrderByCreatedAtDesc(viaggiatoreId)
                .stream()
                .map(RecensioneMapper::toResponse)
                .toList();
    }

    @Override
    public RecensioneResponse modificaRecensione(UUID recensioneId,
                                                 UUID viaggiatoreId,
                                                 RecensioneUpdateRequest request) {

        Recensione recensione = recensioneRepository.findById(recensioneId)
                .orElseThrow(() -> new RecensioneNotFoundException(recensioneId));

        if (!recensione.getViaggiatoreId().equals(viaggiatoreId)) {
            log.warn("Modifica negata su recensione {}: richiesta da {}, appartiene a {}",
                    recensioneId, viaggiatoreId, recensione.getViaggiatoreId());
            throw new AccessDeniedException("Recensione non accessibile");
        }


        LocalDateTime scadenza = recensione.getCreatedAt().plusDays(modificaEntroGiorni);
        if (LocalDateTime.now().isAfter(scadenza)) {
            throw new StatoRecensioneException(
                    "Recensione non più modificabile: scaduto il termine di "
                            + modificaEntroGiorni + " giorni dalla creazione");
        }

        recensione.setValutazione(request.getValutazione());
        recensione.setTitolo(request.getTitolo());
        recensione.setCommento(request.getCommento());

        Recensione saved = recensioneRepository.save(recensione);

        log.info("Recensione {} modificata", recensioneId);

        return RecensioneMapper.toResponse(saved);
    }

    @Override
    public void eliminaRecensione(UUID recensioneId, UUID viaggiatoreId) {

        Recensione recensione = recensioneRepository.findById(recensioneId)
                .orElseThrow(() -> new RecensioneNotFoundException(recensioneId));

        if (!recensione.getViaggiatoreId().equals(viaggiatoreId)) {
            log.warn("Eliminazione negata su recensione {}: richiesta da {}, appartiene a {}",
                    recensioneId, viaggiatoreId, recensione.getViaggiatoreId());
            throw new AccessDeniedException("Recensione non accessibile");
        }

        recensioneRepository.delete(recensione);

        log.info("Recensione {} eliminata", recensioneId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecensioneResponse> trovaRecensioniOggetto(UUID oggettoId) {
        return recensioneRepository
                .findByOggettoIdOrderByCreatedAtDesc(oggettoId)
                .stream()
                .map(RecensioneMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Double mediaValutazione(UUID oggettoId) {
        return recensioneRepository.mediaValutazione(oggettoId);
    }

    @Override
    @Transactional(readOnly = true)
    public long contaRecensioni(UUID oggettoId) {
        return recensioneRepository.countByOggettoId(oggettoId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean puoRecensire(UUID viaggiatoreId,
                                UUID prenotazioneId,
                                TipoOggetto tipoOggetto,
                                UUID oggettoId) {

        try {
            verificaRecensibilita(viaggiatoreId, prenotazioneId, tipoOggetto, oggettoId);

            //anche i duplicati impediscono di recensire di nuovo.
            if (recensioneRepository.findByPrenotazioneId(prenotazioneId).isPresent()) {
                return false;
            }
            if (recensioneRepository
                    .findByViaggiatoreIdAndOggettoId(viaggiatoreId, oggettoId)
                    .isPresent()) {
                return false;
            }
            return true;

        } catch (ReviewException | AccessDeniedException e) {
            log.debug("puoRecensire=false per viaggiatore={}, prenotazione={}, oggetto={}: {}",
                    viaggiatoreId, prenotazioneId, oggettoId, e.getMessage());
            return false;
        }
    }


    //helper privati

    private void verificaRecensibilita(UUID viaggiatoreId,
                                       UUID prenotazioneId,
                                       TipoOggetto tipoOggetto,
                                       UUID oggettoId) {

        PrenotazioneResponseDTO prenotazione = recuperaPrenotazione(prenotazioneId, viaggiatoreId);

        if (prenotazione.getStato() != StatoPrenotazione.COMPLETATA) {
            throw new StatoRecensioneException(
                    "Impossibile recensire: prenotazione in stato " + prenotazione.getStato()
                            + " (richiesto COMPLETATA)");
        }

        if (tipoOggetto == TipoOggetto.VIAGGIO) {
            if (!oggettoId.equals(prenotazione.getViaggioId())) {
                throw new ReviewException(
                        "L'oggetto recensito non corrisponde al viaggio prenotato");
            }
        } else { //Attivita
            boolean attivitaPresente = prenotazione.getAttivitaSelezionate() != null
                    && prenotazione.getAttivitaSelezionate().stream()
                    .anyMatch(a -> oggettoId.equals(a.getAttivitaId()));
            if (!attivitaPresente) {
                throw new ReviewException(
                        "L'attività recensita non è inclusa nella prenotazione");
            }
        }
    }

    private PrenotazioneResponseDTO recuperaPrenotazione(UUID prenotazioneId, UUID viaggiatoreId) {
        try {
            return bookingClient.getPrenotazione(prenotazioneId);

        } catch (FeignException.NotFound e) {
            throw new ReviewException(
                    "Prenotazione " + prenotazioneId + " non trovata su booking-service");

        } catch (FeignException.Forbidden e) {
            log.warn("Booking ha negato l'accesso a prenotazione {} per viaggiatore {}",
                    prenotazioneId, viaggiatoreId);
            throw new AccessDeniedException("Prenotazione non accessibile");

        } catch (FeignException e) {
            log.error("Errore comunicazione con booking-service per prenotazione {}",
                    prenotazioneId, e);
            throw new ReviewException("Errore nel verificare la prenotazione");
        }
    }

    private String recuperaNomeOggetto(TipoOggetto tipo, UUID oggettoId) {
        try {
            return tipo == TipoOggetto.VIAGGIO
                    ? catalogClient.getTrip(oggettoId).getName()
                    : catalogClient.getActivity(oggettoId).getName();

        } catch (FeignException.NotFound e) {
            throw new ReviewException(
                    (tipo == TipoOggetto.VIAGGIO ? "Viaggio " : "Attività ")
                            + oggettoId + " non trovato su catalog-service");

        } catch (FeignException e) {
            log.error("Errore comunicazione con catalog-service per {} {}", tipo, oggettoId, e);
            throw new ReviewException("Errore nel recuperare i dati dell'oggetto");
        }
    }
}