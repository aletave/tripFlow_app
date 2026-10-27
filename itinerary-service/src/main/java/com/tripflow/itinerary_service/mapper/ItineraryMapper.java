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

@Component
public class ItineraryMapper {

    //Converte un DTO di richiesta (CreateItineraryRequest)in una Entity Itinerary pronta per il db
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

    //Converte il DTO AddItineraryStopRequest in una Entity ItineraryStop.

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

    //Converte una Entity Itinerary in un DTO Response da restituire al frontend.
    public ItineraryResponse toResponse(Itinerary itinerary) {
        return toResponse(itinerary, false);
    }

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

    // Vista ridotta per il link pubblico: nessun dato sull'owner o sui permessi.
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

    // I timestamp tecnici (createdAt/updatedAt) sono salvati in UTC come LocalDateTime:
    // in uscita si esplicita l'offset (es. "2026-10-27T11:15:00Z") cosi' il client
    // puo' convertirli correttamente nell'ora italiana senza ambiguita'.
    public static OffsetDateTime toUtc(LocalDateTime utcDateTime) {
        return utcDateTime != null ? utcDateTime.atOffset(ZoneOffset.UTC) : null;
    }

    //Converte una singola Entity ItineraryStop in un DTO ItineraryStopResponse.
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

    public void updateEntity(UpdateItineraryRequest request, Itinerary itinerary) {
        itinerary.setTitle(request.title());
        itinerary.setDescription(request.description());
        itinerary.setStartDate(request.startDate());
        itinerary.setEndDate(request.endDate());
    }

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