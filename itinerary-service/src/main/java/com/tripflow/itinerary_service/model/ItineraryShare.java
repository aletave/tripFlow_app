package com.tripflow.itinerary_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}