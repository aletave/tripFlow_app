package com.tripflow.itinerary_service.repository;

import com.tripflow.itinerary_service.model.ItineraryStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

/**
 * Accesso alle tappe. Le operazioni normali passano dall'itinerario; qui ci sono
 * le cancellazioni in blocco usate quando il catalogo elimina un viaggio.
 */
public interface ItineraryStopRepository extends JpaRepository<ItineraryStop, Long> {

    /**
     * Elimina in blocco le tappe {@code TRIP} che puntano al viaggio indicato.
     *
     * @return numero di tappe eliminate
     */
    @Modifying
    @Query("delete from ItineraryStop s where s.viaggioId = :viaggioId")
    int deleteByViaggioId(@Param("viaggioId") UUID viaggioId);

    /**
     * Elimina in blocco le tappe {@code ACTIVITY} che puntano alle attivita' indicate.
     *
     * @return numero di tappe eliminate
     */
    @Modifying
    @Query("delete from ItineraryStop s where s.attivitaId in :attivitaIds")
    int deleteByAttivitaIdIn(@Param("attivitaIds") Collection<UUID> attivitaIds);
}
