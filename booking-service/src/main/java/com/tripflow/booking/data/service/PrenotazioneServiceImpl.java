package com.tripflow.booking.data.service;

import com.tripflow.booking.client.CatalogClient;
import com.tripflow.booking.client.dto.ActivityResponseDTO;
import com.tripflow.booking.client.dto.TripResponseDTO;
import com.tripflow.booking.config.handler.GlobalExceptionHandler;
import com.tripflow.booking.configSecurity.UtenteAutenticato;
import com.tripflow.booking.data.dao.PrenotazioneRepository;
import com.tripflow.booking.data.dao.PrenotazioneAttivitaRepository;
import com.tripflow.booking.data.dao.PrenotazioneSpecification;
import com.tripflow.booking.data.dto.requests.PrenotazioneAttivitaRequest;
import com.tripflow.booking.data.dto.requests.PrenotazioneRequest;
import com.tripflow.booking.data.dto.responses.PrenotazioneResponse;
import com.tripflow.booking.data.entities.Prenotazione;
import com.tripflow.booking.data.entities.PrenotazioneAttivita;
import com.tripflow.booking.data.entities.enums.StatoPrenotazione;
import com.tripflow.booking.data.service.events.PagamentoCompletatoEvent;
import com.tripflow.booking.data.service.events.PrenotazioneAnnullataEvent;
import com.tripflow.booking.exception.BookingException;
import com.tripflow.booking.exception.PrenotazioneNotFoundException;
import com.tripflow.booking.exception.RisorsaNonTrovataException;
import com.tripflow.booking.exception.ServizioNonDisponibileException;
import com.tripflow.booking.exception.StatoPrenotazioneException;
import com.tripflow.booking.mapper.PrenotazioneMapper;
import com.tripflow.booking.config.PrenotazioneProperties;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.print.Book;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PrenotazioneServiceImpl implements PrenotazioneService {

    private static final String ORGANIZER = "ORGANIZER";
    private static final long LOCK_JOB_SCADENZE = 810_001L;
    private static final long LOCK_JOB_COMPLETAMENTI = 810_002L;

    private final PrenotazioneRepository prenotazioneRepository;
    private final PrenotazioneAttivitaRepository attivitaRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PrenotazioneProperties prenotazioneProperties;
    private final CatalogClient catalogClient;


    @Override
    public PrenotazioneResponse creaPrenotazione(UUID viaggiatoreId, PrenotazioneRequest request) {

        log.info("Creazione prenotazione: viaggiatore={}, viaggio={}, partecipanti={}",
                viaggiatoreId, request.getViaggioId(), request.getNumeroPartecipanti());

        TripResponseDTO viaggio = recuperaViaggio(request.getViaggioId());

        if(!viaggio.getStartDate().isAfter(LocalDate.now())){
            throw new BookingException(
                    "Impossibile prenotare: il viaggio " + viaggio.getId()
                        + " è iniziato il " + viaggio.getStartDate()
            );
        }

        LocalDateTime adesso = LocalDateTime.now();

        prenotazioneRepository.bloccaViaggio(request.getViaggioId().toString());

        int occupati = prenotazioneRepository.sommaPostiOccupati(
                request.getViaggioId(), adesso);
        int richiesti = request.getNumeroPartecipanti();
        int disponibili = viaggio.getAvailableSpots() - occupati;

        if (richiesti > disponibili) {
            throw new BookingException(
                    "Posti insufficienti per il viaggio " + request.getViaggioId() +
                            ": disponibili " + disponibili +
                            ", richiesti " + richiesti);
        }


        List<ActivityResponseDTO> attivitaRichieste = filtraAttivitaRichieste(
                viaggio, request.getAttivitaIds());

        //check disponibilità posti per ogni attività richiesta
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
                .dataPrenotazione(adesso)
                .scadenzaIl(adesso.plus(prenotazioneProperties.ttl()))
                .note(request.getNote())
                .prezzoTotale(BigDecimal.ZERO) //placeholder, ricalcolato sotto
                .build();

        for (ActivityResponseDTO att : attivitaRichieste) {
            PrenotazioneAttivita pa = PrenotazioneAttivita.builder()
                    .attivitaId(att.getId())
                    .attivitaNomeSnap(att.getName())
                    .attivitaPrezzoSnap(att.getPrice())
                    .attivitaDurataSnap(att.getDuration())
                    .build();
            prenotazione.aggiungiAttivita(pa);
        }

        prenotazione.ricalcolaPrezzoTotale();
        Prenotazione saved = prenotazioneRepository.save(prenotazione);

        log.info("Prenotazione creata: id={}, prezzoTotale={}, attivita={}, scade il {}",
                saved.getId(), saved.getPrezzoTotale(), saved.getAttivitaSelezionate().size(), saved.getScadenzaIl());

        return PrenotazioneMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PrenotazioneResponse trovaPrenotazione(UUID prenotazioneId, UUID viaggiatoreId) {

        Prenotazione prenotazione = prenotazioneRepository.trovaConAttivita(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

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
    public List<PrenotazioneResponse> trovaPrenotazioniPerViaggio(UUID viaggioId,
                                                                  UUID organizzatoreId) {

        TripResponseDTO viaggio = recuperaViaggio(viaggioId);
        if (!organizzatoreId.equals(viaggio.getOrganizerId())) {
            log.warn("Vista prenotazioni negata su viaggio {}: richiesto da {}, organizzatore reale {}",
                    viaggioId, organizzatoreId, viaggio.getOrganizerId());
            throw new AccessDeniedException("Viaggio non gestito da questo organizzatore");
        }

        return prenotazioneRepository.findByViaggioId(viaggioId)
                .stream()
                .map(PrenotazioneMapper::toResponse)
                .toList();
    }

    @Override
    public PrenotazioneResponse annullaPrenotazione(UUID prenotazioneId, UUID viaggiatoreId) {

        Prenotazione prenotazione = prenotazioneRepository.trovaConAttivita(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        //check ownership
        if (!prenotazione.getViaggiatoreId().equals(viaggiatoreId)) {
            log.warn("Annullamento negato su prenotazione {}: richiesto da {}, appartiene a {}",
                    prenotazioneId, viaggiatoreId, prenotazione.getViaggiatoreId());
            throw new AccessDeniedException("Prenotazione non accessibile");
        }

        StatoPrenotazione statoAttuale = prenotazione.getStato();
        if (statoAttuale != StatoPrenotazione.IN_ATTESA
                && statoAttuale != StatoPrenotazione.CONFERMATA) {
            throw new StatoPrenotazioneException(
                    "Impossibile annullare: prenotazione in stato " + statoAttuale);
        }

        boolean eraConfermata = (statoAttuale == StatoPrenotazione.CONFERMATA);

        prenotazione.setStato(StatoPrenotazione.ANNULLATA);
        Prenotazione saved = prenotazioneRepository.save(prenotazione);

        log.info("Prenotazione {} annullata (eraConfermata={})", prenotazioneId, eraConfermata);

        eventPublisher.publishEvent(new PrenotazioneAnnullataEvent(prenotazioneId, eraConfermata));

        return PrenotazioneMapper.toResponse(saved);
    }

    @Override
    public PrenotazioneResponse aggiungiAttivita(UUID prenotazioneId,
                                                 UUID viaggiatoreId,
                                                 PrenotazioneAttivitaRequest request) {

        Prenotazione prenotazione = prenotazioneRepository.trovaConAttivita(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        if (!prenotazione.getViaggiatoreId().equals(viaggiatoreId)) {
            log.warn("Modifica negata su prenotazione {}: richiesto da {}, appartiene a {}",
                    prenotazioneId, viaggiatoreId, prenotazione.getViaggiatoreId());
            throw new AccessDeniedException("Prenotazione non accessibile");
        }

        if (prenotazione.getStato() != StatoPrenotazione.IN_ATTESA) {
            throw new StatoPrenotazioneException(
                    "Impossibile aggiungere attività: prenotazione in stato " + prenotazione.getStato());
        }

        //Check duplicato
        UUID attivitaId = request.getAttivitaId();
        boolean giaPresente = prenotazione.getAttivitaSelezionate().stream()
                .anyMatch(a -> a.getAttivitaId().equals(attivitaId));
        if (giaPresente) {
            throw new StatoPrenotazioneException(
                    "Attività " + attivitaId + " già presente nella prenotazione");
        }

        ActivityResponseDTO att = recuperaAttivita(attivitaId);


        if (!att.getTripId().equals(prenotazione.getViaggioId())) {
            throw new BookingException(
                    "Attività " + attivitaId + " non appartiene al viaggio " +
                            prenotazione.getViaggioId());
        }

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

        prenotazione.aggiungiAttivita(nuovaAttivita);
        prenotazione.ricalcolaPrezzoTotale();

        Prenotazione saved = prenotazioneRepository.save(prenotazione);

        log.info("Attività {} aggiunta a prenotazione {}, nuovo prezzoTotale={}",
                attivitaId, prenotazioneId, saved.getPrezzoTotale());

        return PrenotazioneMapper.toResponse(saved);
    }

    @Override
    public PrenotazioneResponse rimuoviAttivita(UUID prenotazioneId,
                                                UUID viaggiatoreId,
                                                UUID attivitaId) {

        Prenotazione prenotazione = prenotazioneRepository.trovaConAttivita(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        if (!prenotazione.getViaggiatoreId().equals(viaggiatoreId)) {
            log.warn("Modifica negata su prenotazione {}: richiesto da {}, appartiene a {}",
                    prenotazioneId, viaggiatoreId, prenotazione.getViaggiatoreId());
            throw new AccessDeniedException("Prenotazione non accessibile");
        }

        if (prenotazione.getStato() != StatoPrenotazione.IN_ATTESA) {
            throw new StatoPrenotazioneException(
                    "Impossibile rimuovere attività: prenotazione in stato " + prenotazione.getStato());
        }

        PrenotazioneAttivita daRimuovere = prenotazione.getAttivitaSelezionate().stream()
                .filter(a -> a.getAttivitaId().equals(attivitaId))
                .findFirst()
                .orElseThrow(() -> new StatoPrenotazioneException(
                        "Attività " + attivitaId + " non presente nella prenotazione"));

        prenotazione.rimuoviAttivita(daRimuovere);
        prenotazione.ricalcolaPrezzoTotale();

        Prenotazione saved = prenotazioneRepository.save(prenotazione);

        log.info("Attività {} rimossa da prenotazione {}, nuovo prezzoTotale={}",
                attivitaId, prenotazioneId, saved.getPrezzoTotale());

        return PrenotazioneMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrenotazioneResponse> ricerca(UtenteAutenticato utente,
                                              UUID viaggiatoreId,
                                              UUID viaggioId,
                                              StatoPrenotazione stato,
                                              LocalDateTime da,
                                              LocalDateTime a,
                                              BigDecimal prezzoMin,
                                              BigDecimal prezzoMax) {

        boolean isOrganizer = ORGANIZER.equals(utente.ruolo());

        Specification<Prenotazione> scope;

        if (isOrganizer) {
            scope = scopeOrganizzatore(utente.id(), viaggioId);
        } else {
            scope = PrenotazioneSpecification.viaggiatoreEquals(utente.id());
        }

        UUID idViaggiatore;
        UUID idViaggio;

        if (isOrganizer) {
            idViaggiatore = viaggiatoreId;
            idViaggio = null;
        } else {
            idViaggiatore = null;
            idViaggio = viaggioId;
        }

        Specification<Prenotazione> filtri = Specification.allOf(
                //un viaggiatore non può filtrare su altri viaggiatori
                PrenotazioneSpecification.viaggiatoreEquals(idViaggiatore),
                PrenotazioneSpecification.viaggioEquals(idViaggio),
                PrenotazioneSpecification.hasStato(stato),
                PrenotazioneSpecification.prenotataTra(da, a),
                PrenotazioneSpecification.prezzoMaggioreDi(prezzoMin),
                PrenotazioneSpecification.prezzoMinoreDi(prezzoMax)
        );

        List<Prenotazione> risultati =
                prenotazioneRepository.findAll(Specification.allOf(scope, filtri));

        log.debug("Ricerca prenotazioni: utente={} ruolo={} -> {} risultati " +
                        "[viaggiatore={}, viaggio={}, stato={}, da={}, a={}, prezzoMin={}, prezzoMax={}]",
                utente.id(), utente.ruolo(), risultati.size(),
                viaggiatoreId, viaggioId, stato, da, a, prezzoMin, prezzoMax);

        return risultati.stream()
                .map(PrenotazioneMapper::toResponse)
                .toList();
    }


    @Override
    public PrenotazioneResponse confermaPrenotazione(UUID prenotazioneId) {

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

        prenotazione.setStato(StatoPrenotazione.CONFERMATA);
        Prenotazione saved = prenotazioneRepository.save(prenotazione);

        log.info("Prenotazione {} confermata", prenotazioneId);

        return PrenotazioneMapper.toResponse(saved);
    }


    @EventListener
    public void onPagamentoCompletato(PagamentoCompletatoEvent event) {
        log.debug("Ricevuto evento PagamentoCompletato per prenotazione {}",
                event.prenotazioneId());

        confermaPrenotazione(event.prenotazioneId());
    }

    @Override
    @Scheduled(cron = "0 0 3 * * *")
    public int completaPrenotazioniScadute() {

        if (!Boolean.TRUE.equals(prenotazioneRepository.provaAcquisireLockJob(LOCK_JOB_COMPLETAMENTI))) {
            log.debug("Job completamenti gia' in esecuzione altrove, salto il giro");
            return 0;
        }

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

    @Override
    @Scheduled(fixedDelayString = "${booking.prenotazione.frequenza-pulizia:PT1M}")
    public int scadutePrenotazioniNonPagate() {

        if (!Boolean.TRUE.equals(prenotazioneRepository.provaAcquisireLockJob(LOCK_JOB_SCADENZE))) {
            log.debug("Job scadenze gia' in esecuzione altrove, salto il giro");
            return 0;
        }

        List<Prenotazione> scaduti = prenotazioneRepository.trovaHoldScaduti(LocalDateTime.now());

        if (scaduti.isEmpty()) {
            return 0;
        }

        for (Prenotazione p : scaduti) {
            p.setStato(StatoPrenotazione.SCADUTA);
            log.info("Prenotazione {} scaduta: creata il {}, scadeva il {}",
                    p.getId(), p.getDataPrenotazione(), p.getScadenzaIl());
        }

        prenotazioneRepository.saveAll(scaduti);

        log.info("Scadute {} prenotazioni non pagate", scaduti.size());

        return scaduti.size();
    }


    //helper privati integrazione catalog

    private TripResponseDTO recuperaViaggio(UUID viaggioId) {
        try {
            return catalogClient.getTrip(viaggioId);
        } catch (FeignException.NotFound e) {
            throw new RisorsaNonTrovataException("Viaggio non trovato nel catalog: " + viaggioId);
        } catch (FeignException e) {
            log.error("Errore comunicazione con catalog-service per viaggio {}",
                    viaggioId, e);
            throw new ServizioNonDisponibileException(
                    "Impossibile recuperare il viaggio: catalog-service non disponibile");
        }
    }

    private ActivityResponseDTO recuperaAttivita(UUID attivitaId) {
        try {
            return catalogClient.getActivity(attivitaId);
        } catch (FeignException.NotFound e) {
            throw new RisorsaNonTrovataException("Attività non trovata nel catalog: " + attivitaId);
        } catch (FeignException e) {
            log.error("Errore comunicazione con catalog-service per attività {}",
                    attivitaId, e);
            throw new ServizioNonDisponibileException(
                    "Impossibile recuperare l'attività: catalog-service non disponibile");
        }
    }

    //organizzatore, solo le prenotazioni dei viaggi che organizza lui
    private Specification<Prenotazione> scopeOrganizzatore(UUID organizzatoreId, UUID viaggioId) {

        if (viaggioId != null) {
            TripResponseDTO viaggio = recuperaViaggio(viaggioId);
            if (!organizzatoreId.equals(viaggio.getOrganizerId())) {
                log.warn("Ricerca negata su viaggio {}: richiesta da {}, organizzatore reale {}",
                        viaggioId, organizzatoreId, viaggio.getOrganizerId());
                throw new AccessDeniedException("Viaggio non gestito da questo organizzatore");
            }
            return PrenotazioneSpecification.viaggioEquals(viaggioId);
        }

        List<UUID> suoiViaggi = recuperaViaggiDiOrganizzatore(organizzatoreId).stream()
                .map(TripResponseDTO::getId)
                .toList();

        return PrenotazioneSpecification.viaggioIdIn(suoiViaggi);
    }

    private List<TripResponseDTO> recuperaViaggiDiOrganizzatore(UUID organizzatoreId) {
        try {
            return catalogClient.getTripsByOrganizer(organizzatoreId);
        } catch (FeignException e) {
            log.error("Errore comunicazione con catalog-service per i viaggi dell'organizzatore {}",
                    organizzatoreId, e);
            throw new ServizioNonDisponibileException(
                    "Impossibile recuperare i viaggi dell'organizzatore: catalog-service non disponibile");
        }
    }


    private List<ActivityResponseDTO> filtraAttivitaRichieste(TripResponseDTO viaggio,
                                                              List<UUID> attivitaIds) {
        if (attivitaIds == null || attivitaIds.isEmpty()) {
            return List.of();
        }

        List<ActivityResponseDTO> tutte;
        if (viaggio.getActivities() != null) {
            tutte = viaggio.getActivities();
        }
        else{
            tutte = List.of();
        }


        List<ActivityResponseDTO> selezionate = new ArrayList<>();
        for (UUID id : attivitaIds.stream().distinct().toList()) {
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