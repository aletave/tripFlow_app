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

    // Snapshot del viaggio (Dati congelati)
    private String titoloViaggio;
    private String destinazione;
    private LocalDate dataInizio;
    private LocalDate dataFine;
    private BigDecimal prezzoUnitarioAlMomentoDelBooking;

    // Dati calcolati e stato
    private Integer numeroPartecipanti;
    private BigDecimal prezzoTotale;
    private StatoPrenotazione stato;
    private LocalDateTime dataPrenotazione;
    private String note;

    // Dettagli relazioni (usando altri Response DTO)
    private List<PrenotazioneAttivitaResponse> attivitaSelezionate;
    private PagamentoResponse infoPagamento;

    // Audit
    private LocalDateTime createdAt;
}