package com.tripflow.booking.mapper;

import com.tripflow.booking.data.dto.responses.PrenotazioneAttivitaResponse;
import com.tripflow.booking.data.dto.responses.PrenotazioneResponse;
import com.tripflow.booking.data.entities.Prenotazione;
import com.tripflow.booking.data.entities.PrenotazioneAttivita;

import java.util.List;

/**
 * Conversioni Entity -> DTO per Prenotazione e PrenotazioneAttivita.
 *
 * Solo direzione entity -> DTO: il senso opposto (Request -> Entity)
 * lo fa direttamente il service, perché serve accesso al catalog
 * per popolare gli snapshot, e quindi non è una mappatura "pura".
 */
public final class PrenotazioneMapper {

    private PrenotazioneMapper() {
    }

    public static PrenotazioneResponse toResponse(Prenotazione p) {
        if (p == null) return null;

        List<PrenotazioneAttivitaResponse> attivita = p.getAttivitaSelezionate() == null
                ? List.of()
                : p.getAttivitaSelezionate().stream()
                .map(PrenotazioneMapper::toAttivitaResponse)
                .toList();

        return PrenotazioneResponse.builder()
                .id(p.getId())
                .viaggiatoreId(p.getViaggiatoreId())
                .viaggioId(p.getViaggioId())
                .titoloViaggio(p.getViaggioTitoloSnap())
                .destinazione(p.getViaggioDestinazioneSnap())
                .dataInizio(p.getViaggioDataInizioSnap())
                .dataFine(p.getViaggioDataFineSnap())
                .prezzoUnitarioAlMomentoDelBooking(p.getViaggioPrezzoSnap())
                .numeroPartecipanti(p.getNumeroPartecipanti())
                .prezzoTotale(p.getPrezzoTotale())
                .stato(p.getStato())
                .dataPrenotazione(p.getDataPrenotazione())
                .note(p.getNote())
                .attivitaSelezionate(attivita)
                .infoPagamento(PagamentoMapper.toResponse(p.getPagamento()))
                .createdAt(p.getCreatedAt())
                .build();
    }

    public static PrenotazioneAttivitaResponse toAttivitaResponse(PrenotazioneAttivita a) {
        if (a == null) return null;
        return PrenotazioneAttivitaResponse.builder()
                .id(a.getId())
                .attivitaId(a.getAttivitaId())
                .nome(a.getAttivitaNomeSnap())
                .prezzo(a.getAttivitaPrezzoSnap())
                .durataMinuti(a.getAttivitaDurataSnap())
                .aggiuntoIl(a.getCreatedAt())
                .build();
    }
}