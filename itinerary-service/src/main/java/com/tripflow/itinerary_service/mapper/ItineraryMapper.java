package com.tripflow.itinerary_service.mapper;

import com.tripflow.itinerary_service.dto.request.AddItineraryStopRequest;
import com.tripflow.itinerary_service.dto.request.CreateItineraryRequest;
import com.tripflow.itinerary_service.dto.request.UpdateItineraryRequest;
import com.tripflow.itinerary_service.dto.request.UpdateItineraryStopRequest;
import com.tripflow.itinerary_service.dto.response.ItineraryResponse;
import com.tripflow.itinerary_service.dto.response.ItineraryStopResponse;
import com.tripflow.itinerary_service.dto.response.PublicItineraryResponse;
import com.tripflow.itinerary_service.model.Itinerary;
import com.tripflow.itinerary_service.model.ItineraryStop;
import com.tripflow.itinerary_service.model.Visibility;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Conversioni tra entity JPA e DTO di itinerari e tappe.
 */
@Component
public class ItineraryMapper {

    /**
     * Crea l'entity di un nuovo itinerario. La visibilita' arriva gia' risolta dal service.
     */
    public Itinerary toEntity(CreateItineraryRequest request, UUID ownerId, Visibility visibility) {

        Itinerary itinerary = new Itinerary();

        itinerary.setTitle(request.title());
        itinerary.setDescription(request.description());
        itinerary.setVisibility(visibility);
        itinerary.setStartDate(request.startDate());
        itinerary.setEndDate(request.endDate());

        itinerary.setOwnerId(ownerId);

        return itinerary;
    }

    /**
     * Crea l'entity di una nuova tappa collegata all'itinerario.
     */

    public ItineraryStop toStopEntity(
            AddItineraryStopRequest request,
            Itinerary itinerary
    ) {
        ItineraryStop stop = new ItineraryStop();


        stop.setStopOrder(request.stopOrder());

        stop.setStopType(request.stopType());

        stop.setViaggioId(request.viaggioId());
        stop.setAttivitaId(request.attivitaId());

        stop.setCustomTitle(request.customTitle());
        stop.setCustomDescription(request.customDescription());


        stop.setStartDatetime(request.startDatetime());
        stop.setEndDatetime(request.endDatetime());

        stop.setNotes(request.notes());


        stop.setItinerary(itinerary);

        return stop;
    }

    /**
     * Converte un itinerario nella risposta per l'app.
     *
     * @param canEdit se l'utente corrente puo' modificarlo (calcolato dal service)
     */
    public ItineraryResponse toResponse(Itinerary itinerary, boolean canEdit) {

        List<ItineraryStopResponse> stops =
                itinerary.getStops()
                        .stream()
                        .map(this::toStopResponse)
                        .toList();

        return new ItineraryResponse(
                itinerary.getId(),
                itinerary.getOwnerId(),
                itinerary.getTitle(),
                itinerary.getDescription(),
                itinerary.getVisibility(),
                itinerary.getStartDate(),
                itinerary.getEndDate(),
                toUtc(itinerary.getCreatedAt()),
                toUtc(itinerary.getUpdatedAt()),
                stops,
                itinerary.getPublicToken(),
                canEdit
        );
    }

    /**
     * Vista ridotta per l'accesso pubblico: nessun dato sul proprietario o sui permessi.
     */
    public PublicItineraryResponse toPublicResponse(Itinerary itinerary) {
        return new PublicItineraryResponse(
                itinerary.getId(),
                itinerary.getTitle(),
                itinerary.getDescription(),
                itinerary.getStartDate(),
                itinerary.getEndDate(),
                itinerary.getStops().stream().map(this::toStopResponse).toList()
        );
    }

    /**
     * Aggiunge l'offset UTC a un timestamp salvato in UTC (es. {@code 2026-10-27T11:15:00Z}),
     * cosi' il client lo converte senza ambiguita' nell'ora locale.
     */
    public static OffsetDateTime toUtc(LocalDateTime utcDateTime) {
        return utcDateTime != null ? utcDateTime.atOffset(ZoneOffset.UTC) : null;
    }

    /**
     * Converte una tappa nella risposta per l'app.
     */
    public ItineraryStopResponse toStopResponse(ItineraryStop stop) {

        return new ItineraryStopResponse(
                stop.getId(),
                stop.getStopOrder(),
                stop.getStopType(),
                stop.getViaggioId(),
                stop.getAttivitaId(),
                stop.getCustomTitle(),
                stop.getCustomDescription(),
                stop.getStartDatetime(),
                stop.getEndDatetime(),
                stop.getNotes()
        );
    }

    /**
     * Sostituisce titolo, descrizione e date dell'itinerario.
     */
    public void updateEntity(UpdateItineraryRequest request, Itinerary itinerary) {
        itinerary.setTitle(request.title());
        itinerary.setDescription(request.description());
        itinerary.setStartDate(request.startDate());
        itinerary.setEndDate(request.endDate());
    }

    /**
     * Applica una modifica parziale alla tappa: i campi {@code null} restano invariati.
     * Se cambia il tipo, i riferimenti al catalogo vengono sostituiti da quelli della richiesta.
     */
    public void updateStopEntity(UpdateItineraryStopRequest request, ItineraryStop stop) {
        if (request.stopOrder() != null) {
            stop.setStopOrder(request.stopOrder());
        }
        if (request.stopType() != null && !request.stopType().equals(stop.getStopType())) {
            // Cambio di tipo: i riferimenti precedenti non sono piu' coerenti
            // (es. TRIP -> CUSTOM) e vengono sostituiti da quelli della richiesta.
            stop.setStopType(request.stopType());
            stop.setViaggioId(request.viaggioId());
            stop.setAttivitaId(request.attivitaId());
        } else {
            if (request.viaggioId() != null) {
                stop.setViaggioId(request.viaggioId());
            }
            if (request.attivitaId() != null) {
                stop.setAttivitaId(request.attivitaId());
            }
        }
        if (request.customTitle() != null) {
            stop.setCustomTitle(request.customTitle());
        }
        if (request.customDescription() != null) {
            stop.setCustomDescription(request.customDescription());
        }
        if (request.startDatetime() != null) {
            stop.setStartDatetime(request.startDatetime());
        }
        if (request.endDatetime() != null) {
            stop.setEndDatetime(request.endDatetime());
        }
        if (request.notes() != null) {
            stop.setNotes(request.notes());
        }
    }
}