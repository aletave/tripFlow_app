package com.tripflow.itinerary_service.mapper;

import com.tripflow.itinerary_service.dto.request.AddItineraryStopRequest;
import com.tripflow.itinerary_service.dto.request.CreateItineraryRequest;
import com.tripflow.itinerary_service.dto.response.ItineraryResponse;
import com.tripflow.itinerary_service.dto.response.ItineraryStopResponse;
import com.tripflow.itinerary_service.model.Itinerary;
import com.tripflow.itinerary_service.model.ItineraryStop;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ItineraryMapper {

    //Converte un DTO di richiesta (CreateItineraryRequest)in una Entity Itinerary pronta per il db
    public Itinerary toEntity(CreateItineraryRequest request, UUID ownerId) {

        Itinerary itinerary = new Itinerary();

        // Copia dati dal DTO alla entity
        itinerary.setTitle(request.title());
        itinerary.setDescription(request.description());
        itinerary.setVisibility(request.visibility());
        itinerary.setStartDate(request.startDate());
        itinerary.setEndDate(request.endDate());

        itinerary.setOwnerId(ownerId);

        // Restituisce la entity completa
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
                itinerary.getCreatedAt(),
                itinerary.getUpdatedAt(),
                stops
        );
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
}