package com.tripflow.booking.data.dao;

import com.tripflow.booking.data.entities.Pagamento;
import com.tripflow.booking.data.entities.enums.StatoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {


    Optional<Pagamento> findByPrenotazioneId(UUID prenotazioneId);

    Optional<Pagamento> findByStripePaymentIntentId(String stripePaymentIntentId);

    List<Pagamento> findByStato(StatoPagamento stato);

    List<Pagamento> findByStatoAndDataPagamentoBetween(
            StatoPagamento stato,
            LocalDateTime inizio,
            LocalDateTime fine
    );

    long countByStato(StatoPagamento stato);

    @Query("""
           SELECT COUNT(pg) > 0 FROM Pagamento pg
           WHERE pg.codiceSconto = :codice
             AND pg.prenotazione.viaggiatoreId = :viaggiatoreId
             AND pg.prenotazione.id <> :prenotazioneId
             AND (pg.stato = com.tripflow.booking.data.entities.enums.StatoPagamento.COMPLETATO
                  OR (pg.prenotazione.stato = com.tripflow.booking.data.entities.enums.StatoPrenotazione.IN_ATTESA
                      AND pg.prenotazione.scadenzaIl > :adesso))
           """)
    boolean codiceGiaUsatoDaViaggiatore(@Param("codice") String codice,
                                        @Param("viaggiatoreId") UUID viaggiatoreId,
                                        @Param("prenotazioneId") UUID prenotazioneId,
                                        @Param("adesso") LocalDateTime adesso);
}