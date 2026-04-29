package com.tripflow.itinerary_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "itineraries")
public class Itinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 30)
    private String visibility;

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
    private List<ItineraryStop> stops = new ArrayList<>();

    // Costruttore vuoto
    public Itinerary() {
    }

    // viene eseguito prima di salvare per la prima volta un oggetto nel DB
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // viene eseguito prima di aggiornare un oggetto nel DB
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}