package com.tripflow.booking.data.dao;

import com.tripflow.booking.data.entities.Prenotazione;
import com.tripflow.booking.data.entities.enums.StatoPrenotazione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrenotazioneRepository extends JpaRepository<Prenotazione, UUID>,
        JpaSpecificationExecutor<Prenotazione> {

    // query methods (derived queries)

    // "Le mie prenotazioni" — ordinate dalla più recente
    List<Prenotazione> findByViaggiatoreIdOrderByDataPrenotazioneDesc(UUID viaggiatoreId);

    // "Le mie prenotazioni attive" — filtrate per stati (es. IN_ATTESA + CONFERMATA)
    List<Prenotazione> findByViaggiatoreIdAndStatoIn(UUID viaggiatoreId,
                                                     Collection<StatoPrenotazione> stati);

    // "Prenotazioni ricevute per il mio viaggio" — vista organizzatore
    List<Prenotazione> findByViaggioId(UUID viaggioId);

    // Tutte le prenotazioni in un certo stato (es. job batch)
    List<Prenotazione> findByStato(StatoPrenotazione stato);

    // "Ho già prenotato questo viaggio?" — esclude le prenotazioni annullate
    List<Prenotazione> findByViaggiatoreIdAndViaggioIdAndStatoIn(
            UUID viaggiatoreId, UUID viaggioId, Collection<StatoPrenotazione> stati);



    // QUERY JPQL CUSTOM

    // Carica prenotazione + attività in una sola query (evita N+1).
    // LEFT JOIN FETCH: include la prenotazione anche se non ha attività.
    // DISTINCT: evita righe duplicate dovute al join.
    @Query("""
           SELECT DISTINCT p FROM Prenotazione p
           LEFT JOIN FETCH p.attivitaSelezionate
           WHERE p.id = :id
           """)
    Optional<Prenotazione> trovaConAttivita(@Param("id") UUID id);

    // Somma dei partecipanti per un viaggio (esclude le annullate).
    // COALESCE evita NULL quando non ci sono prenotazioni: ritorna 0.
    @Query("""
           SELECT COALESCE(SUM(p.numeroPartecipanti), 0) FROM Prenotazione p
           WHERE p.viaggioId = :viaggioId
             AND p.stato <> com.tripflow.booking.data.entities.enums.StatoPrenotazione.ANNULLATA
           """)
    Integer sommaPartecipantiPerViaggio(@Param("viaggioId") UUID viaggioId);

    // Prenotazioni "scadute" da completare: confermate con viaggio già finito.
    // Usata da un job schedulato che le sposta in COMPLETATA.
    @Query("""
           SELECT p FROM Prenotazione p
           WHERE p.stato = com.tripflow.booking.data.entities.enums.StatoPrenotazione.CONFERMATA
             AND p.viaggioDataFineSnap < CURRENT_DATE
           """)
    List<Prenotazione> trovaDaCompletare();
}