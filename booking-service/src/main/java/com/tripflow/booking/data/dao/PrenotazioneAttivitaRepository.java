package com.tripflow.booking.data.dao;

import com.tripflow.booking.data.entities.PrenotazioneAttivita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}