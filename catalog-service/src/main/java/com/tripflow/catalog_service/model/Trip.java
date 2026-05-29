package com.tripflow.catalog_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trips")
public class Trip {

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
    @Column(name = "destination", nullable = false)
    private String destination;

    @Getter
    @Setter
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Getter
    @Setter
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Getter
    @Setter
    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Getter
    @Setter
    @Column(name = "available_spots", nullable = false)
    private int availableSpots;

    @Getter
    @Setter
    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Getter
    @Setter
    @Column(name = "description", nullable = false)
    private String description;

    @Getter
    @Setter
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "images", columnDefinition = "jsonb")
    private List<String> images = new ArrayList<>();

    @Getter
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Activity> activities = new ArrayList<>();

    public Trip() {}

    public void addActivity(Activity a) {
        activities.add(a);
        a.setTrip(this);
    }

    public void removeActivity(Activity a) {
        activities.remove(a);
        a.setTrip(null);
    }

}
