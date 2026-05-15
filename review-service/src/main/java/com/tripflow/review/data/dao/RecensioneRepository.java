package com.tripflow.review.data.dao;

import com.tripflow.review.data.entities.Recensione;
import com.tripflow.review.data.entities.enums.TipoOggetto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecensioneRepository extends JpaRepository<Recensione, UUID> {

    List<Recensione> findByOggettoIdOrderByCreatedAtDesc(UUID oggettoId);

    List<Recensione> findByViaggiatoreIdOrderByCreatedAtDesc(UUID viaggiatoreId);

    Optional<Recensione> findByPrenotazioneId(UUID prenotazioneId);

    Optional<Recensione> findByViaggiatoreIdAndOggettoId(UUID viaggiatoreId, UUID oggettoId);

    long countByOggettoId(UUID oggettoId);

    List<Recensione> findByTipoOggetto(TipoOggetto tipoOggetto);


    //custom

    // Media stelle per un oggetto.
    // COALESCE evita NULL quando non ci sono recensioni: ritorna 0.
    @Query("""
           SELECT COALESCE(AVG(r.valutazione), 0) FROM Recensione r
           WHERE r.oggettoId = :oggettoId
           """)
    Double mediaValutazione(@Param("oggettoId") UUID oggettoId);
}