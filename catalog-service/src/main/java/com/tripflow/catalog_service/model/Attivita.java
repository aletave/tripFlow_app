package com.tripflow.catalog_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "attivita")
public class Attivita {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Getter
    @Setter
    @Column(name = "nome", nullable = false)
    private String nome;

    @Getter
    @Setter
    @Column(name = "descrizione", nullable = false)
    private String descrizione;

    @Getter
    @Setter
    @Column(name = "durata", nullable = false)
    private Integer durata;

    @Getter
    @Setter
    @Column(name = "prezzo", nullable = false)
    private BigDecimal prezzo;

    @Getter
    @Setter
    @Column(name = "posti_disponibili", nullable = false)
    private Integer postiDisponibili;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_viaggio", nullable = false)
    private Viaggio viaggio;

    public Attivita() {}


}
