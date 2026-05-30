package com.tripflow.booking.data.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "prenotazione_attivita",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_prenotazione_attivita",
                columnNames = {"prenotazione_id", "attivita_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrenotazioneAttivita {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;


    //relazione con Prenotazione (lato padrone)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prenotazione_id", nullable = false)
    private Prenotazione prenotazione;

    //riferimento al service catalog
    @Column(name = "attivita_id", nullable = false)
    private UUID attivitaId;


    // dati attività
    @Column(name = "attivita_nome_snap", nullable = false, length = 255)
    private String attivitaNomeSnap;

    @Column(name = "attivita_prezzo_snap", nullable = false, precision = 10, scale = 2)
    private BigDecimal attivitaPrezzoSnap;

    @Column(name = "attivita_durata_snap", nullable = false)
    private Integer attivitaDurataSnap;



    // AUDIT

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}