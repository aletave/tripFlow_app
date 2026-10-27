package com.tripflow.itinerary_service.repository;

import com.tripflow.itinerary_service.model.ItineraryStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ItineraryStopRepository extends JpaRepository<ItineraryStop, Long> {

    // tutte le tappe di un itinerario ordinate
    List<ItineraryStop> findByItineraryIdOrderByStopOrderAsc(Long itineraryId);

    // elimina tutte le tappe di un itinerario
    void deleteByItineraryId(Long itineraryId);

    // Pulizia delle tappe orfane quando il catalogo elimina un viaggio:
    // DELETE diretta (bulk), senza caricare le entity una per una.
    @Modifying
    @Query("delete from ItineraryStop s where s.viaggioId = :viaggioId")
    int deleteByViaggioId(@Param("viaggioId") UUID viaggioId);

    // Tappe ACTIVITY che puntano ad attivita' del viaggio eliminato.
    @Modifying
    @Query("delete from ItineraryStop s where s.attivitaId in :attivitaIds")
    int deleteByAttivitaIdIn(@Param("attivitaIds") Collection<UUID> attivitaIds);
}
