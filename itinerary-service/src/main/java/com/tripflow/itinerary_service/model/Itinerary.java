package com.tripflow.itinerary_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Itinerario di un utente (tabella {@code itineraries}).
 *
 * <p>Possiede le sue tappe: salvandolo o eliminandolo si salvano o eliminano anche loro.
 * {@code publicToken} esiste solo quando la visibilita' e' {@code PUBLIC}.
 * I timestamp sono in UTC e vengono impostati automaticamente.
 */
@Getter
@Setter
@Entity
@Table(name = "itineraries")
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Visibility visibility;

    @Column(name = "public_token", unique = true)
    private UUID publicToken;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Tappe in ordine di stopOrder; rimuoverne una dalla lista la elimina dal DB.
    @OneToMany(
            mappedBy = "itinerary",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("stopOrder ASC")
    private List<ItineraryStop> stops = new ArrayList<>();

    public Itinerary() {
    }

    /** Imposta i timestamp al primo salvataggio. */
    @PrePersist
    public void prePersist() {
        // Timestamp sempre in UTC: la conversione in ora locale spetta al client.
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /** Aggiorna {@code updatedAt} a ogni modifica. */
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }


}