package com.tripflow.bookingservice.data.dao;

import com.tripflow.booking.data.entities.Prenotazione;
import com.tripflow.booking.data.entities.enums.StatoPrenotazione;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Helper di Specification per ricerca dinamica su Prenotazione.
 *
 * Pattern d'uso nel service:
 *   Specification<Prenotazione> spec = Specification.where(viaggiatoreEquals(id))
 *                                                   .and(hasStato(stato))
 *                                                   .and(prenotataTra(da, a));
 *   List<Prenotazione> risultati = repository.findAll(spec);
 *
 * Ogni metodo restituisce null se il parametro è null:
 * questo permette di concatenare filtri opzionali senza if/else nel service.
 * Specification.where(null) è gestito correttamente da Spring.
 */
public final class PrenotazioneSpecification {

    private PrenotazioneSpecification() {
        // classe utility, singleton, non istanziabile
    }

    public static Specification<Prenotazione> viaggiatoreEquals(UUID viaggiatoreId) {
        if (viaggiatoreId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("viaggiatoreId"), viaggiatoreId);
    }

    public static Specification<Prenotazione> viaggioEquals(UUID viaggioId) {
        if (viaggioId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("viaggioId"), viaggioId);
    }

    public static Specification<Prenotazione> hasStato(StatoPrenotazione stato) {
        if (stato == null) return null;
        return (root, query, cb) -> cb.equal(root.get("stato"), stato);
    }

    public static Specification<Prenotazione> prenotataTra(LocalDateTime da, LocalDateTime a) {
        if (da == null && a == null) return null;
        return (root, query, cb) -> {
            if (da != null && a != null) {
                return cb.between(root.get("dataPrenotazione"), da, a);
            } else if (da != null) {
                return cb.greaterThanOrEqualTo(root.get("dataPrenotazione"), da);
            } else {
                return cb.lessThanOrEqualTo(root.get("dataPrenotazione"), a);
            }
        };
    }

    public static Specification<Prenotazione> prezzoMaggioreDi(BigDecimal soglia) {
        if (soglia == null) return null;
        return (root, query, cb) -> cb.greaterThan(root.get("prezzoTotale"), soglia);
    }

    public static Specification<Prenotazione> prezzoMinoreDi(BigDecimal soglia) {
        if (soglia == null) return null;
        return (root, query, cb) -> cb.lessThan(root.get("prezzoTotale"), soglia);
    }
}