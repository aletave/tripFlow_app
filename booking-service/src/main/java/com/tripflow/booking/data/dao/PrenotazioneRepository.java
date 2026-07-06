package com.tripflow.booking.data.dao;

import com.tripflow.booking.data.entities.Prenotazione;
import com.tripflow.booking.data.entities.enums.StatoPrenotazione;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
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


    // "Le mie prenotazioni" — ordinate dalla più recente
    @EntityGraph(attributePaths = {"attivitaSelezionate", "pagamento"})
    List<Prenotazione> findByViaggiatoreIdOrderByDataPrenotazioneDesc(UUID viaggiatoreId);

    // "Le mie prenotazioni attive" — filtrate per stati (es. IN_ATTESA + CONFERMATA)
    @EntityGraph(attributePaths = {"attivitaSelezionate", "pagamento"})
    List<Prenotazione> findByViaggiatoreIdAndStatoIn(UUID viaggiatoreId,
                                                     Collection<StatoPrenotazione> stati);

    // "Prenotazioni ricevute per il mio viaggio" — vista organizzatore
    @EntityGraph(attributePaths = {"attivitaSelezionate", "pagamento"})
    List<Prenotazione> findByViaggioId(UUID viaggioId);

    // Ricerca dinamica: ridichiarare findAll(Specification) permette di
    // agganciare l'@EntityGraph anche alle query da Specification
    @Override
    @EntityGraph(attributePaths = {"attivitaSelezionate", "pagamento"})
    List<Prenotazione> findAll(Specification<Prenotazione> spec);

    // Tutte le prenotazioni in un certo stato
    List<Prenotazione> findByStato(StatoPrenotazione stato);

    List<Prenotazione> findByViaggiatoreIdAndViaggioIdAndStatoIn(
            UUID viaggiatoreId, UUID viaggioId, Collection<StatoPrenotazione> stati);



    // QUERY JPQL CUSTOM

    //carica prenotazione + attività in una sola query (evita N+1)
    @Query("""
           SELECT DISTINCT p FROM Prenotazione p
           LEFT JOIN FETCH p.attivitaSelezionate
           WHERE p.id = :id
           """)
    Optional<Prenotazione> trovaConAttivita(@Param("id") UUID id);

    //somma dei partecipanti per un viaggio (esclude le annullate)
    @Query("""
           SELECT COALESCE(SUM(p.numeroPartecipanti), 0) FROM Prenotazione p
           WHERE p.viaggioId = :viaggioId
             AND p.stato <> com.tripflow.booking.data.entities.enums.StatoPrenotazione.ANNULLATA
           """)
    Integer sommaPartecipantiPerViaggio(@Param("viaggioId") UUID viaggioId);


    @Query("""
           SELECT p FROM Prenotazione p
           WHERE p.stato = com.tripflow.booking.data.entities.enums.StatoPrenotazione.CONFERMATA
             AND p.viaggioDataFineSnap < CURRENT_DATE
           """)
    List<Prenotazione> trovaDaCompletare();

    //evita race condition sullo stesso viaggio
    @Query(value = """
           SELECT COUNT(*) FROM (
               SELECT pg_advisory_xact_lock(hashtext(:viaggioId))
           ) AS lock_acquisito
           """, nativeQuery = true)
    Long bloccaViaggio(@Param("viaggioId") String viaggioId);
}