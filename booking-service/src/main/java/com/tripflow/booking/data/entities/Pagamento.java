package com.tripflow.booking.data.entities;

import com.tripflow.booking.data.entities.enums.MetodoPagamento;
import com.tripflow.booking.data.entities.enums.StatoPagamento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;


    //relazione con Prenotazione (lato padrone, 1:1)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prenotazione_id", nullable = false, unique = true)
    private Prenotazione prenotazione;


    // dati pagamento

    @Column(name = "importo", nullable = false, precision = 10, scale = 2)
    private BigDecimal importo;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo", nullable = true, length = 20)
    private MetodoPagamento metodo;

    @Enumerated(EnumType.STRING)
    @Column(name = "stato", nullable = false, length = 20)
    private StatoPagamento stato;

    //riferimenti stripe (non dati sensibili)
    //opzionali per design
    @Column(name = "stripe_payment_intent_id", unique = true, length = 100)
    private String stripePaymentIntentId;

    @Column(name = "ultime_quattro_cifre", length = 4)
    private String ultimeQuattroCifre;

    @Column(name = "brand_carta", length = 20)
    private String brandCarta;

    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento;


    //audit
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}