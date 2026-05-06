package com.tripflow.itinerary_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

//si occuperà lombok per fare getter e setter in maniera automatico.
@Getter
@Setter
@Entity
@Table(name = "itinerary_stops")
public class ItineraryStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Column(name = "stop_type", nullable = false, length = 30)
    private String stopType;

    @Column(name = "viaggio_id")
    private Long viaggioId;
    @Column(name = "attivita_id")
    private Long attivitaId;

    @Column(name = "custom_title", length = 150)
    private String customTitle;

    @Column(name = "custom_description", columnDefinition = "TEXT")
    private String customDescription;

    @Column(name = "start_datetime")
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime")
    private LocalDateTime endDatetime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    //costruttore vuoto
    public ItineraryStop() {
    }

    //gestisce la relazione tra itinerary stop (1) -> itineary(n)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;



}