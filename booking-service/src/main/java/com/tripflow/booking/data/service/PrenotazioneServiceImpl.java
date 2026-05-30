package com.tripflow.booking.data.service;

import com.tripflow.booking.client.CatalogClient;
import com.tripflow.booking.client.dto.ActivityResponseDTO;
import com.tripflow.booking.client.dto.TripResponseDTO;
import com.tripflow.booking.data.dao.PrenotazioneRepository;
import com.tripflow.booking.data.dao.PrenotazioneAttivitaRepository;
import com.tripflow.booking.data.dao.PrenotazioneSpecification;
import com.tripflow.booking.data.dto.requests.PrenotazioneAttivitaRequest;
import com.tripflow.booking.data.dto.requests.PrenotazioneRequest;
import com.tripflow.booking.data.dto.responses.PrenotazioneResponse;
import com.tripflow.booking.data.entities.Prenotazione;
import com.tripflow.booking.data.entities.PrenotazioneAttivita;
import com.tripflow.booking.data.entities.enums.StatoPrenotazione;
import com.tripflow.booking.data.service.events.PrenotazioneAnnullataEvent;
import com.tripflow.booking.exception.BookingException;
import com.tripflow.booking.exception.PrenotazioneNotFoundException;
import com.tripflow.booking.exception.StatoPrenotazioneException;
import com.tripflow.booking.mapper.PrenotazioneMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PrenotazioneServiceImpl implements PrenotazioneService {

    private final PrenotazioneRepository prenotazioneRepository;
    private final PrenotazioneAttivitaRepository attivitaRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CatalogClient catalogClient;


    @Override
    public PrenotazioneResponse creaPrenotazione(UUID viaggiatoreId, PrenotazioneRequest request) {

        log.info("Creazione prenotazione: viaggiatore={}, viaggio={}, partecipanti={}",
                viaggiatoreId, request.getViaggioId(), request.getNumeroPartecipanti());

        //Recupero dati viaggio (e attività) dal catalog..
        TripResponseDTO viaggio = recuperaViaggio(request.getViaggioId());

        //Check disponibilità posti.
        Integer giaPrenotati = prenotazioneRepository.sommaPartecipantiPerViaggio(request.getViaggioId());
        int richiesti = request.getNumeroPartecipanti();
        if (giaPrenotati + richiesti > viaggio.getAvailableSpots()) {
            throw new BookingException(
                    "Posti insufficienti per il viaggio " + request.getViaggioId() +
                            ": disponibili " + (viaggio.getAvailableSpots() - giaPrenotati) +
                            ", richiesti " + richiesti);
        }

        //Filtra le attività richieste dalla lista già restituita dal catalog.
        List<ActivityResponseDTO> attivitaRichieste = filtraAttivitaRichieste(
                viaggio, request.getAttivitaIds());

        // Check disponibilità posti per ogni attività richiesta.
        //TODO: in futuro confrontare con la somma dei partecipanti già
        // prenotati per quella specifica attività.
        for (ActivityResponseDTO att : attivitaRichieste) {
            if (richiesti > att.getAvailableSpots()) {
                throw new BookingException(
                        "Posti insufficienti per l'attività " + att.getId() +
                                ": disponibili " + att.getAvailableSpots() +
                                ", richiesti " + richiesti);
            }
        }

        // 5. Costruzione entity Prenotazione con snapshot dati dal catalog.
        Prenotazione prenotazione = Prenotazione.builder()
                .viaggiatoreId(viaggiatoreId)
                .viaggioId(viaggio.getId())
                .viaggioTitoloSnap(viaggio.getName())
                .viaggioDestinazioneSnap(viaggio.getDestination())
                .viaggioDataInizioSnap(viaggio.getStartDate())
                .viaggioDataFineSnap(viaggio.getEndDate())
                .viaggioPrezzoSnap(viaggio.getPrice())
                .numeroPartecipanti(richiesti)
                .stato(StatoPrenotazione.IN_ATTESA)
                .dataPrenotazione(LocalDateTime.now())
                .note(request.getNote())
                .prezzoTotale(BigDecimal.ZERO) // placeholder, ricalcolato sotto
                .build();

        //Snapshot attività dai dati reali del catalog.
        for (ActivityResponseDTO att : attivitaRichieste) {
            PrenotazioneAttivita pa = PrenotazioneAttivita.builder()
                    .attivitaId(att.getId())
                    .attivitaNomeSnap(att.getName())
                    .attivitaPrezzoSnap(att.getPrice())
                    .attivitaDurataSnap(att.getDuration())
                    .build();
            prenotazione.aggiungiAttivita(pa);
        }

        //Calcolo prezzo totale.
        prenotazione.ricalcolaPrezzoTotale();

        //save
        Prenotazione saved = prenotazioneRepository.save(prenotazione);

        log.info("Prenotazione creata: id={}, prezzoTotale={}, attivita={}",
                saved.getId(), saved.getPrezzoTotale(), saved.getAttivitaSelezionate().size());

        return PrenotazioneMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PrenotazioneResponse trovaPrenotazione(UUID prenotazioneId, UUID viaggiatoreId) {

        Prenotazione prenotazione = prenotazioneRepository.trovaConAttivita(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        // Check ownership
        if (!prenotazione.getViaggiatoreId().equals(viaggiatoreId)) {
            log.warn("Accesso negato a prenotazione {}: richiesto da {}, appartiene a {}",
                    prenotazioneId, viaggiatoreId, prenotazione.getViaggiatoreId());
            throw new AccessDeniedException("Prenotazione non accessibile");
        }

        return PrenotazioneMapper.toResponse(prenotazione);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrenotazioneResponse> trovaMiePrenotazioni(UUID viaggiatoreId) {
        return prenotazioneRepository
                .findByViaggiatoreIdOrderByDataPrenotazioneDesc(viaggiatoreId)
                .stream()
                .map(PrenotazioneMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrenotazioneResponse> trovaMiePrenotazioniAttive(UUID viaggiatoreId) {
        List<StatoPrenotazione> statiAttivi = List.of(
                StatoPrenotazione.IN_ATTESA,
                StatoPrenotazione.CONFERMATA
        );
        return prenotazioneRepository
                .findByViaggiatoreIdAndStatoIn(viaggiatoreId, statiAttivi)
                .stream()
                .map(PrenotazioneMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrenotazioneResponse> trovaPrenotazioniPerViaggio(UUID viaggioId) {
        return prenotazioneRepository.findByViaggioId(viaggioId)
                .stream()
                .map(PrenotazioneMapper::toResponse)
                .toList();
    }

    @Override
    public PrenotazioneResponse annullaPrenotazione(UUID prenotazioneId, UUID viaggiatoreId) {

        //Carico (con attività per il mapping finale)
        Prenotazione prenotazione = prenotazioneRepository.trovaConAttivita(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        //Check ownership
        if (!prenotazione.getViaggiatoreId().equals(viaggiatoreId)) {
            log.warn("Annullamento negato su prenotazione {}: richiesto da {}, appartiene a {}",
                    prenotazioneId, viaggiatoreId, prenotazione.getViaggiatoreId());
            throw new AccessDeniedException("Prenotazione non accessibile");
        }

        //Check stato: annullabili solo IN_ATTESA e CONFERMATA
        StatoPrenotazione statoAttuale = prenotazione.getStato();
        if (statoAttuale != StatoPrenotazione.IN_ATTESA
                && statoAttuale != StatoPrenotazione.CONFERMATA) {
            throw new StatoPrenotazioneException(
                    "Impossibile annullare: prenotazione in stato " + statoAttuale);
        }

        boolean eraConfermata = (statoAttuale == StatoPrenotazione.CONFERMATA);

        //Cambio stato e salvo
        prenotazione.setStato(StatoPrenotazione.ANNULLATA);
        Prenotazione saved = prenotazioneRepository.save(prenotazione);

        log.info("Prenotazione {} annullata (eraConfermata={})", prenotazioneId, eraConfermata);

        //Pubblico evento.
        //Listener sincrono nella stessa transazione: se il rimborso fallisce,
        //l'intera operazione di annullamento viene rollback.
        eventPublisher.publishEvent(new PrenotazioneAnnullataEvent(prenotazioneId, eraConfermata));

        return PrenotazioneMapper.toResponse(saved);
    }

    @Override
    public PrenotazioneResponse aggiungiAttivita(UUID prenotazioneId,
                                                 UUID viaggiatoreId,
                                                 PrenotazioneAttivitaRequest request) {

        //Carico la prenotazione con le attività (serve per ricalcolare il prezzo)
        Prenotazione prenotazione = prenotazioneRepository.trovaConAttivita(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        //Check ownership
        if (!prenotazione.getViaggiatoreId().equals(viaggiatoreId)) {
            log.warn("Modifica negata su prenotazione {}: richiesto da {}, appartiene a {}",
                    prenotazioneId, viaggiatoreId, prenotazione.getViaggiatoreId());
            throw new AccessDeniedException("Prenotazione non accessibile");
        }

        //Check stato: si può modificare solo se IN_ATTESA
        if (prenotazione.getStato() != StatoPrenotazione.IN_ATTESA) {
            throw new StatoPrenotazioneException(
                    "Impossibile aggiungere attività: prenotazione in stato " + prenotazione.getStato());
        }

        //Check duplicato: l'attività non deve essere già presente.
        UUID attivitaId = request.getAttivitaId();
        boolean giaPresente = prenotazione.getAttivitaSelezionate().stream()
                .anyMatch(a -> a.getAttivitaId().equals(attivitaId));
        if (giaPresente) {
            throw new StatoPrenotazioneException(
                    "Attività " + attivitaId + " già presente nella prenotazione");
        }

        //Chiamo il catalog per i dati reali dell'attività.
        ActivityResponseDTO att = recuperaAttivita(attivitaId);

        //Coerenza: l'attività deve appartenere allo stesso viaggio della
        //prenotazione, altrimenti l'utente potrebbe aggiungere attività
        //casuali da altri viaggi.
        if (!att.getTripId().equals(prenotazione.getViaggioId())) {
            throw new BookingException(
                    "Attività " + attivitaId + " non appartiene al viaggio " +
                            prenotazione.getViaggioId());
        }

        //Check posti attività.
        if (prenotazione.getNumeroPartecipanti() > att.getAvailableSpots()) {
            throw new BookingException(
                    "Posti insufficienti per l'attività " + att.getId() +
                            ": disponibili " + att.getAvailableSpots() +
                            ", richiesti " + prenotazione.getNumeroPartecipanti());
        }

        PrenotazioneAttivita nuovaAttivita = PrenotazioneAttivita.builder()
                .attivitaId(att.getId())
                .attivitaNomeSnap(att.getName())
                .attivitaPrezzoSnap(att.getPrice())
                .attivitaDurataSnap(att.getDuration())
                .build();

        //Aggiungo (helper bidirezionale) e ricalcolo il prezzo
        prenotazione.aggiungiAttivita(nuovaAttivita);
        prenotazione.ricalcolaPrezzoTotale();

        //Save: il cascade salva anche la nuova PrenotazioneAttivita
        Prenotazione saved = prenotazioneRepository.save(prenotazione);

        log.info("Attività {} aggiunta a prenotazione {}, nuovo prezzoTotale={}",
                attivitaId, prenotazioneId, saved.getPrezzoTotale());

        return PrenotazioneMapper.toResponse(saved);
    }

    @Override
    public PrenotazioneResponse rimuoviAttivita(UUID prenotazioneId,
                                                UUID viaggiatoreId,
                                                UUID attivitaId) {

        //Carico con attività
        Prenotazione prenotazione = prenotazioneRepository.trovaConAttivita(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        //Check ownership
        if (!prenotazione.getViaggiatoreId().equals(viaggiatoreId)) {
            log.warn("Modifica negata su prenotazione {}: richiesto da {}, appartiene a {}",
                    prenotazioneId, viaggiatoreId, prenotazione.getViaggiatoreId());
            throw new AccessDeniedException("Prenotazione non accessibile");
        }

        //Check stato
        if (prenotazione.getStato() != StatoPrenotazione.IN_ATTESA) {
            throw new StatoPrenotazioneException(
                    "Impossibile rimuovere attività: prenotazione in stato " + prenotazione.getStato());
        }

        //Trovo l'attività da rimuovere nella collezione
        PrenotazioneAttivita daRimuovere = prenotazione.getAttivitaSelezionate().stream()
                .filter(a -> a.getAttivitaId().equals(attivitaId))
                .findFirst()
                .orElseThrow(() -> new StatoPrenotazioneException(
                        "Attività " + attivitaId + " non presente nella prenotazione"));

        //Rimuovo (helper bidirezionale: orphanRemoval cancellerà la riga in DB)
        //e ricalcolo il prezzo
        prenotazione.rimuoviAttivita(daRimuovere);
        prenotazione.ricalcolaPrezzoTotale();

        //Save
        Prenotazione saved = prenotazioneRepository.save(prenotazione);

        log.info("Attività {} rimossa da prenotazione {}, nuovo prezzoTotale={}",
                attivitaId, prenotazioneId, saved.getPrezzoTotale());

        return PrenotazioneMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrenotazioneResponse> ricerca(UUID viaggiatoreId,
                                              UUID viaggioId,
                                              StatoPrenotazione stato,
                                              LocalDateTime da,
                                              LocalDateTime a,
                                              BigDecimal prezzoMin,
                                              BigDecimal prezzoMax) {


        Specification<Prenotazione> spec = Specification.allOf(
                PrenotazioneSpecification.viaggiatoreEquals(viaggiatoreId),
                PrenotazioneSpecification.viaggioEquals(viaggioId),
                PrenotazioneSpecification.hasStato(stato),
                PrenotazioneSpecification.prenotataTra(da, a),
                PrenotazioneSpecification.prezzoMaggioreDi(prezzoMin),
                PrenotazioneSpecification.prezzoMinoreDi(prezzoMax)
        );

        List<Prenotazione> risultati = prenotazioneRepository.findAll(spec);

        log.debug("Ricerca prenotazioni: trovate {} con filtri " +
                        "[viaggiatore={}, viaggio={}, stato={}, da={}, a={}, prezzoMin={}, prezzoMax={}]",
                risultati.size(), viaggiatoreId, viaggioId, stato, da, a, prezzoMin, prezzoMax);

        return risultati.stream()
                .map(PrenotazioneMapper::toResponse)
                .toList();
    }

    @Override
    public PrenotazioneResponse confermaPrenotazione(UUID prenotazioneId) {

        //Carico (con attività per il response finale)
        Prenotazione prenotazione = prenotazioneRepository.trovaConAttivita(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        StatoPrenotazione statoAttuale = prenotazione.getStato();

        if (statoAttuale == StatoPrenotazione.CONFERMATA) {
            log.info("Prenotazione {} già CONFERMATA, ignoro la conferma duplicata", prenotazioneId);
            return PrenotazioneMapper.toResponse(prenotazione);
        }

        if (statoAttuale != StatoPrenotazione.IN_ATTESA) {
            throw new StatoPrenotazioneException(
                    "Impossibile confermare: prenotazione in stato " + statoAttuale);
        }

        //Transizione e save
        prenotazione.setStato(StatoPrenotazione.CONFERMATA);
        Prenotazione saved = prenotazioneRepository.save(prenotazione);

        log.info("Prenotazione {} confermata", prenotazioneId);

        return PrenotazioneMapper.toResponse(saved);
    }

    @Override
    public int completaPrenotazioniScadute() {

        List<Prenotazione> daCompletare = prenotazioneRepository.trovaDaCompletare();

        if (daCompletare.isEmpty()) {
            log.debug("Nessuna prenotazione da completare");
            return 0;
        }

        for (Prenotazione p : daCompletare) {
            p.setStato(StatoPrenotazione.COMPLETATA);
        }

        prenotazioneRepository.saveAll(daCompletare);

        log.info("Completate {} prenotazioni scadute", daCompletare.size());

        return daCompletare.size();
    }



    //helper privati integrazione catalog

    private TripResponseDTO recuperaViaggio(UUID viaggioId) {
        try {
            return catalogClient.getTrip(viaggioId);
        } catch (FeignException.NotFound e) {
            throw new BookingException("Viaggio non trovato nel catalog: " + viaggioId);
        } catch (FeignException e) {
            log.error("Errore comunicazione con catalog-service per viaggio {}",
                    viaggioId, e);
            throw new BookingException(
                    "Errore comunicazione con catalog-service: " + e.getMessage());
        }
    }

    private ActivityResponseDTO recuperaAttivita(UUID attivitaId) {
        try {
            return catalogClient.getActivity(attivitaId);
        } catch (FeignException.NotFound e) {
            throw new BookingException("Attività non trovata nel catalog: " + attivitaId);
        } catch (FeignException e) {
            log.error("Errore comunicazione con catalog-service per attività {}",
                    attivitaId, e);
            throw new BookingException(
                    "Errore comunicazione con catalog-service: " + e.getMessage());
        }
    }

    private List<ActivityResponseDTO> filtraAttivitaRichieste(TripResponseDTO viaggio,
                                                              List<UUID> attivitaIds) {
        if (attivitaIds == null || attivitaIds.isEmpty()) {
            return List.of();
        }
        List<ActivityResponseDTO> tutte = viaggio.getActivities() != null
                ? viaggio.getActivities()
                : List.of();

        List<ActivityResponseDTO> selezionate = new ArrayList<>();
        for (UUID id : attivitaIds) {
            ActivityResponseDTO trovata = tutte.stream()
                    .filter(a -> a.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new BookingException(
                            "Attività " + id + " non appartiene al viaggio " + viaggio.getId()));
            selezionate.add(trovata);
        }
        return selezionate;
    }
}