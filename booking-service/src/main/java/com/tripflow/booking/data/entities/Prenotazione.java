package com.tripflow.booking.data.entities;

import com.tripflow.booking.data.entities.enums.StatoPrenotazione;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prenotazione")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prenotazione {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "viaggiatore_id", nullable = false)
    private UUID viaggiatoreId;

    @Column(name = "viaggio_id", nullable = false)
    private UUID viaggioId;


    //snapshot dati viaggio
    // (congelati al momento della prenotazione)

    @Column(name = "viaggio_titolo_snap", nullable = false, length = 255)
    private String viaggioTitoloSnap;

    @Column(name = "viaggio_destinazione_snap", nullable = false, length = 255)
    private String viaggioDestinazioneSnap;

    @Column(name = "viaggio_data_inizio_snap", nullable = false)
    private LocalDate viaggioDataInizioSnap;

    @Column(name = "viaggio_data_fine_snap", nullable = false)
    private LocalDate viaggioDataFineSnap;

    @Column(name = "viaggio_prezzo_snap", nullable = false, precision = 10, scale = 2)
    private BigDecimal viaggioPrezzoSnap;


    //dati prenotazione
    @Column(name = "numero_partecipanti", nullable = false)
    private Integer numeroPartecipanti;

    @Column(name = "prezzo_totale", nullable = false, precision = 10, scale = 2)
    private BigDecimal prezzoTotale;

    @Enumerated(EnumType.STRING)
    @Column(name = "stato", nullable = false, length = 20)
    private StatoPrenotazione stato;

    @Column(name = "data_prenotazione", nullable = false)
    private LocalDateTime dataPrenotazione;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;


    //AUDIT
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    //relazioni
    @OneToMany(mappedBy = "prenotazione", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PrenotazioneAttivita> attivitaSelezionate = new ArrayList<>();

    @OneToOne(mappedBy = "prenotazione", cascade = CascadeType.ALL, orphanRemoval = true)
    private Pagamento pagamento;


    // metodi helper
    //setter custom (sovrascrive Lombok per coerenza bidirezionale)

    public void setPagamento(Pagamento pagamento) {
        this.pagamento = pagamento;
        if (pagamento != null) {
            pagamento.setPrenotazione(this);
        }
    }


    //helper per la lista attività

    public void aggiungiAttivita(PrenotazioneAttivita attivita) {
        attivitaSelezionate.add(attivita);
        attivita.setPrenotazione(this);
    }

    public void rimuoviAttivita(PrenotazioneAttivita attivita) {
        attivitaSelezionate.remove(attivita);
        attivita.setPrenotazione(null);
    }

    //Ricalcola il prezzo totale in base allo stato corrente:
    //(prezzoViaggio + somma prezzi attività) * numeroPartecipanti
    public void ricalcolaPrezzoTotale() {
        BigDecimal prezzoUnitario = viaggioPrezzoSnap;
        for (PrenotazioneAttivita a : attivitaSelezionate) {
            prezzoUnitario = prezzoUnitario.add(a.getAttivitaPrezzoSnap());
        }
        this.prezzoTotale = prezzoUnitario.multiply(BigDecimal.valueOf(numeroPartecipanti));
    }
}