package com.tripflow.booking.data.service;

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
import com.tripflow.booking.mapper.PrenotazioneMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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


    @Override
    public PrenotazioneResponse creaPrenotazione(UUID viaggiatoreId, PrenotazioneRequest request) {

        log.info("Creazione prenotazione: viaggiatore={}, viaggio={}, partecipanti={}",
                viaggiatoreId, request.getViaggioId(), request.getNumeroPartecipanti());

        // 1. Snapshot viaggio (TODO: sostituire con chiamata al catalog-service)
        // Per ora dati fittizi indipendenti dal viaggioId richiesto.
        BigDecimal prezzoViaggio = new BigDecimal("100.00");
        String titoloViaggio = "Viaggio XYZ";
        String destinazioneViaggio = "Destinazione Demo";
        LocalDate dataInizioViaggio = LocalDate.now().plusDays(30);
        LocalDate dataFineViaggio = LocalDate.now().plusDays(37);

        // 2. Costruzione entity Prenotazione (senza prezzo totale, lo calcoliamo dopo)
        Prenotazione prenotazione = Prenotazione.builder()
                .viaggiatoreId(viaggiatoreId)
                .viaggioId(request.getViaggioId())
                .viaggioTitoloSnap(titoloViaggio)
                .viaggioDestinazioneSnap(destinazioneViaggio)
                .viaggioDataInizioSnap(dataInizioViaggio)
                .viaggioDataFineSnap(dataFineViaggio)
                .viaggioPrezzoSnap(prezzoViaggio)
                .numeroPartecipanti(request.getNumeroPartecipanti())
                .stato(StatoPrenotazione.IN_ATTESA)
                .dataPrenotazione(LocalDateTime.now())
                .note(request.getNote())
                .prezzoTotale(BigDecimal.ZERO) // placeholder, ricalcolato sotto
                .build();

        // 3. Snapshot attività (TODO: sostituire con chiamate al catalog-service)
        // Aggiungo le attività usando il metodo helper dell'entity per la coerenza bidirezionale.
        if (request.getAttivitaIds() != null) {
            for (UUID attivitaId : request.getAttivitaIds()) {
                PrenotazioneAttivita pa = PrenotazioneAttivita.builder()
                        .attivitaId(attivitaId)
                        .attivitaNomeSnap("Attività Demo")
                        .attivitaPrezzoSnap(new BigDecimal("20.00"))
                        .attivitaDurataSnap(120) // minuti
                        .build();
                prenotazione.aggiungiAttivita(pa);
            }
        }

        // 4. Calcolo prezzo totale: (prezzoViaggio + somma prezzi attività) * partecipanti
        //spostato come helper il Prenotazione.
        prenotazione.ricalcolaPrezzoTotale();

        // 5. Save (cascade ALL salva anche le attività in un colpo solo)
        Prenotazione saved = prenotazioneRepository.save(prenotazione);

        log.info("Prenotazione creata: id={}, prezzoTotale={}, attivita={}",
                saved.getId(), saved.getPrezzoTotale(), saved.getAttivitaSelezionate().size());

        return PrenotazioneMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PrenotazioneResponse trovaPrenotazione(UUID prenotazioneId, UUID viaggiatoreId) {

        // Uso trovaConAttivita: LEFT JOIN FETCH evita il problema N+1
        // e permette al mapper di accedere a getAttivitaSelezionate() senza lazy loading.
        Prenotazione prenotazione = prenotazioneRepository.trovaConAttivita(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        // Check ownership: regola di business, sta nel service.
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

        // 1. Carico (con attività per il mapping finale)
        Prenotazione prenotazione = prenotazioneRepository.trovaConAttivita(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        // 2. Check ownership
        if (!prenotazione.getViaggiatoreId().equals(viaggiatoreId)) {
            log.warn("Annullamento negato su prenotazione {}: richiesto da {}, appartiene a {}",
                    prenotazioneId, viaggiatoreId, prenotazione.getViaggiatoreId());
            throw new AccessDeniedException("Prenotazione non accessibile");
        }

        // 3. Check stato: annullabili solo IN_ATTESA e CONFERMATA
        StatoPrenotazione statoAttuale = prenotazione.getStato();
        if (statoAttuale != StatoPrenotazione.IN_ATTESA
                && statoAttuale != StatoPrenotazione.CONFERMATA) {
            throw new StatoPrenotazioneException(
                    "Impossibile annullare: prenotazione in stato " + statoAttuale);
        }

        boolean eraConfermata = (statoAttuale == StatoPrenotazione.CONFERMATA);

        // 4. Cambio stato e salvo
        prenotazione.setStato(StatoPrenotazione.ANNULLATA);
        Prenotazione saved = prenotazioneRepository.save(prenotazione);

        log.info("Prenotazione {} annullata (eraConfermata={})", prenotazioneId, eraConfermata);

        // 5. Pubblico evento.
        //    Listener sincrono nella stessa transazione: se il rimborso fallisce,
        //    l'intera operazione di annullamento viene rollback.
        eventPublisher.publishEvent(new PrenotazioneAnnullataEvent(prenotazioneId, eraConfermata));

        return PrenotazioneMapper.toResponse(saved);
    }

    @Override
    public PrenotazioneResponse aggiungiAttivita(UUID prenotazioneId,
                                                 UUID viaggiatoreId,
                                                 PrenotazioneAttivitaRequest request) {

        // 1. Carico la prenotazione con le attività (serve per ricalcolare il prezzo)
        Prenotazione prenotazione = prenotazioneRepository.trovaConAttivita(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        // 2. Check ownership
        if (!prenotazione.getViaggiatoreId().equals(viaggiatoreId)) {
            log.warn("Modifica negata su prenotazione {}: richiesto da {}, appartiene a {}",
                    prenotazioneId, viaggiatoreId, prenotazione.getViaggiatoreId());
            throw new AccessDeniedException("Prenotazione non accessibile");
        }

        // 3. Check stato: si può modificare solo se IN_ATTESA
        if (prenotazione.getStato() != StatoPrenotazione.IN_ATTESA) {
            throw new StatoPrenotazioneException(
                    "Impossibile aggiungere attività: prenotazione in stato " + prenotazione.getStato());
        }

        // 4. Check duplicato: l'attività non deve essere già presente
        // (è anche un constraint DB con UNIQUE(prenotazione_id, attivita_id),
        //  ma è meglio dare un errore di business pulito invece dell'eccezione di constraint)
        UUID attivitaId = request.getAttivitaId();
        boolean giaPresente = prenotazione.getAttivitaSelezionate().stream()
                .anyMatch(a -> a.getAttivitaId().equals(attivitaId));
        if (giaPresente) {
            throw new StatoPrenotazioneException(
                    "Attività " + attivitaId + " già presente nella prenotazione");
        }

        // 5. Snapshot attività (TODO: sostituire con chiamata al catalog-service)
        PrenotazioneAttivita nuovaAttivita = PrenotazioneAttivita.builder()
                .attivitaId(attivitaId)
                .attivitaNomeSnap("Attività Demo")
                .attivitaPrezzoSnap(new BigDecimal("20.00"))
                .attivitaDurataSnap(120)
                .build();

        // 6. Aggiungo (helper bidirezionale) e ricalcolo il prezzo
        prenotazione.aggiungiAttivita(nuovaAttivita);
        prenotazione.ricalcolaPrezzoTotale();

        // 7. Save: il cascade salva anche la nuova PrenotazioneAttivita
        Prenotazione saved = prenotazioneRepository.save(prenotazione);

        log.info("Attività {} aggiunta a prenotazione {}, nuovo prezzoTotale={}",
                attivitaId, prenotazioneId, saved.getPrezzoTotale());

        return PrenotazioneMapper.toResponse(saved);
    }

    @Override
    public PrenotazioneResponse rimuoviAttivita(UUID prenotazioneId,
                                                UUID viaggiatoreId,
                                                UUID attivitaId) {

        // 1. Carico con attività
        Prenotazione prenotazione = prenotazioneRepository.trovaConAttivita(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        // 2. Check ownership
        if (!prenotazione.getViaggiatoreId().equals(viaggiatoreId)) {
            log.warn("Modifica negata su prenotazione {}: richiesto da {}, appartiene a {}",
                    prenotazioneId, viaggiatoreId, prenotazione.getViaggiatoreId());
            throw new AccessDeniedException("Prenotazione non accessibile");
        }

        // 3. Check stato
        if (prenotazione.getStato() != StatoPrenotazione.IN_ATTESA) {
            throw new StatoPrenotazioneException(
                    "Impossibile rimuovere attività: prenotazione in stato " + prenotazione.getStato());
        }

        // 4. Trovo l'attività da rimuovere nella collezione
        PrenotazioneAttivita daRimuovere = prenotazione.getAttivitaSelezionate().stream()
                .filter(a -> a.getAttivitaId().equals(attivitaId))
                .findFirst()
                .orElseThrow(() -> new StatoPrenotazioneException(
                        "Attività " + attivitaId + " non presente nella prenotazione"));

        // 5. Rimuovo (helper bidirezionale: orphanRemoval cancellerà la riga in DB)
        //    e ricalcolo il prezzo
        prenotazione.rimuoviAttivita(daRimuovere);
        prenotazione.ricalcolaPrezzoTotale();

        // 6. Save
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

        // Composizione dinamica: ogni Specification ritorna null se il parametro è null,
        // e Specification.where(null).and(null) viene gestito senza problemi (= "nessun filtro").
        Specification<Prenotazione> spec = Specification
                .where(PrenotazioneSpecification.viaggiatoreEquals(viaggiatoreId))
                .and(PrenotazioneSpecification.viaggioEquals(viaggioId))
                .and(PrenotazioneSpecification.hasStato(stato))
                .and(PrenotazioneSpecification.prenotataTra(da, a))
                .and(PrenotazioneSpecification.prezzoMaggioreDi(prezzoMin))
                .and(PrenotazioneSpecification.prezzoMinoreDi(prezzoMax));

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

        // 1. Carico (con attività per il response finale)
        Prenotazione prenotazione = prenotazioneRepository.trovaConAttivita(prenotazioneId)
                .orElseThrow(() -> new PrenotazioneNotFoundException(prenotazioneId));

        StatoPrenotazione statoAttuale = prenotazione.getStato();

        // 2. se già CONFERMATA, non fare nulla.
        //    Caso reale: webhook Stripe duplicato, retry di un job, ecc.
        if (statoAttuale == StatoPrenotazione.CONFERMATA) {
            log.info("Prenotazione {} già CONFERMATA, ignoro la conferma duplicata", prenotazioneId);
            return PrenotazioneMapper.toResponse(prenotazione);
        }

        // 3. Stato deve essere IN_ATTESA: ogni altra cosa è incoerente.
        if (statoAttuale != StatoPrenotazione.IN_ATTESA) {
            throw new StatoPrenotazioneException(
                    "Impossibile confermare: prenotazione in stato " + statoAttuale);
        }

        // 4. Transizione e save
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

        // saveAll: una sola flush a fine transazione (più efficiente di N save singoli)
        prenotazioneRepository.saveAll(daCompletare);

        log.info("Completate {} prenotazioni scadute", daCompletare.size());

        return daCompletare.size();
    }
}

