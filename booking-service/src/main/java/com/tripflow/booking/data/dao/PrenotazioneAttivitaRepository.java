package com.tripflow.booking.data.dao;

import com.tripflow.booking.data.entities.PrenotazioneAttivita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrenotazioneAttivitaRepository extends JpaRepository<PrenotazioneAttivita, UUID> {

    List<PrenotazioneAttivita> findByPrenotazioneId(UUID prenotazioneId);

    List<PrenotazioneAttivita> findByAttivitaId(UUID attivitaId);

    Optional<PrenotazioneAttivita> findByPrenotazioneIdAndAttivitaId(UUID prenotazioneId, UUID attivitaId);

    long countByPrenotazioneId(UUID prenotazioneId);

    void deleteByPrenotazioneId(UUID prenotazioneId);

    @Query("""
           SELECT COALESCE(SUM(p.numeroPartecipanti), 0) FROM PrenotazioneAttivita a
                JOIN a.prenotazione p
                WHERE a.attivitaId = :attivitaId
                    AND (p.stato = com.tripflow.booking.data.entities.enums.StatoPrenotazione.CONFERMATA
                        OR (p.stato = com.tripflow.booking.data.entities.enums.StatoPrenotazione.IN_ATTESA
                            AND p.scadenzaIl > :adesso))
             """)
    Integer sommaPostiOccupati(@Param("attivitaId") UUID attivitaId,
                               @Param("adesso") LocalDateTime adesso);
}