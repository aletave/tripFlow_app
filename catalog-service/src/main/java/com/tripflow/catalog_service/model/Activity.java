package com.tripflow.catalog_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "activities")
public class Activity {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Getter
    @Setter
    @Column(name = "name", nullable = false)
    private String name;

    @Getter
    @Setter
    @Column(name = "description", nullable = false)
    private String description;

    @Getter
    @Setter
    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Getter
    @Setter
    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Getter
    @Setter
    @Column(name = "available_spots", nullable = false)
    private Integer availableSpots;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    public Activity() {}

}
