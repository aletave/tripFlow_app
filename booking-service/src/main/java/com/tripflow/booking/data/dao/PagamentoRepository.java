package com.tripflow.booking.data.dao;

import com.tripflow.booking.data.entities.Pagamento;
import com.tripflow.booking.data.entities.enums.StatoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
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
}