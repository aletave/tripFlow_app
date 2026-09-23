package com.tripflow.booking.data.dto.responses;

import com.tripflow.booking.data.entities.enums.StatoPrenotazione;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrenotazioneResponse {

    private UUID id;
    private UUID viaggiatoreId;
    private UUID viaggioId;

    //Snapshot del viaggio (Dati congelati)
    private String titoloViaggio;
    private String destinazione;
    private LocalDate dataInizio;
    private LocalDate dataFine;
    private BigDecimal prezzoUnitarioAlMomentoDelBooking;

    //Dati calcolati e stato
    private Integer numeroPartecipanti;
    private BigDecimal prezzoTotale;
    private StatoPrenotazione stato;
    private LocalDateTime dataPrenotazione;
    private LocalDateTime scadenzaIl;

    //Secondi mancanti alla scadenza, calcolati dal server: il client conta
    //alla rovescia da questo numero senza dover interpretare fusi orari.
    //Null quando non c'e' un hold attivo da mostrare.
    private Long secondiAllaScadenza;

    private String note;

    //Dettagli relazioni
    private List<PrenotazioneAttivitaResponse> attivitaSelezionate;
    private PagamentoResponse infoPagamento;

    private LocalDateTime createdAt;
}