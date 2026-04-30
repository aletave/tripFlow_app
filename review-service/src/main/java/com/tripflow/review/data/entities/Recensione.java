package com.tripflow.review.data.entities;

import com.tripflow.review.data.entities.enums.TipoOggetto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "recensione",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_recensione_prenotazione",
                        columnNames = "prenotazione_id"
                ),
                @UniqueConstraint(
                        name = "uq_viaggiatore_oggetto",
                        columnNames = {"viaggiatore_id", "oggetto_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recensione {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;


    //RIFERIMENTI CROSS-SERVICE
    @Column(name = "viaggiatore_id", nullable = false)
    private UUID viaggiatoreId;

    @Column(name = "prenotazione_id", nullable = false)
    private UUID prenotazioneId;


    // OGGETTO RECENSITO (associazione polimorfica)
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_oggetto", nullable = false, length = 20)
    private TipoOggetto tipoOggetto;

    @Column(name = "oggetto_id", nullable = false)
    private UUID oggettoId;


    //Dati SNAPSHOT
    @Column(name = "oggetto_nome_snap", nullable = false, length = 255)
    private String oggettoNomeSnap;

    @Column(name = "autore_nome_snap", nullable = false, length = 255)
    private String autoreNomeSnap;


    //CONTENUTO RECENSIONE
    @Column(name = "valutazione", nullable = false)
    private Short valutazione;

    @Column(name = "titolo", length = 200)
    private String titolo;

    @Column(name = "commento", columnDefinition = "TEXT")
    private String commento;


    // AUDIT
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}