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
@Table(name = "viaggi")
public class Viaggio {

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
    @Column(name = "destinazione", nullable = false)
    private String destinazione;

    @Getter
    @Setter
    @Column(name = "data_inizio", nullable = false)
    private LocalDate dataInizio;

    @Getter
    @Setter
    @Column(name = "data_fine", nullable = false)
    private LocalDate dataFine;

    @Getter
    @Setter
    @Column(name = "prezzo", nullable = false)
    private BigDecimal prezzo;

    @Getter
    @Setter
    @Column(name = "posti_disponibili", nullable = false)
    private int postiDisponibili;

    @Getter
    @Setter
    @Column(name = "id_organizzatore", nullable = false)
    private UUID idOrganizzatore;

    @Getter
    @Setter
    @Column(name = "descrizione", nullable = false)
    private String descrizione;

    @Getter
    @Setter
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "immagini", columnDefinition = "jsonb")
    private List<String> immagini = new ArrayList<>();

    @Getter
    @OneToMany(mappedBy = "viaggio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attivita> attivita = new ArrayList<>();

    public Viaggio() {}

    public void addAttivita(Attivita a) {
        attivita.add(a);
        a.setViaggio(this);
    }

    public void removeAttivita(Attivita a) {
        attivita.remove(a);
        a.setViaggio(null);
    }

}
