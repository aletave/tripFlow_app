package com.tripflow.itinerary_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Condivisione di un itinerario con un utente (tabella {@code itinerary_shares}).
 * Una sola condivisione per coppia itinerario-utente.
 */
@Getter
@Setter
@Entity
@Table(
    name = "itinerary_shares",
    uniqueConstraints = @UniqueConstraint(name = "uk_itinerary_share", columnNames = {"itinerary_id", "shared_with_user_id"})
)
public class ItineraryShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "itinerary_id", nullable = false)
    private Long itineraryId;

    @Column(name = "shared_with_user_id", nullable = false)
    private UUID sharedWithUserId;

    @Column(name = "can_edit", nullable = false)
    private boolean canEdit;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Imposta la data di creazione in UTC. */
    @PrePersist
    public void prePersist() {
        // Timestamp in UTC, coerente con Itinerary.createdAt.
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}