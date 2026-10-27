package com.tripflow.itinerary_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//si occuperà lombok per fare getter e setter in maniera automatico.
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

    // Relazione con itinerary-stops.
    @OneToMany(
            mappedBy = "itinerary",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("stopOrder ASC")
    private List<ItineraryStop> stops = new ArrayList<>();

    // Costruttore vuoto
    public Itinerary() {
    }

    // viene eseguito prima di salvare per la prima volta un oggetto nel DB
    @PrePersist
    public void prePersist() {
        // Timestamp sempre in UTC: la conversione in ora locale spetta al client.
        createdAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
        updatedAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
    }

    // viene eseguito prima di aggiornare un oggetto nel DB
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
    }


}