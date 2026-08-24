package com.tripflow.itinerary_service.service;

import com.tripflow.itinerary_service.dto.request.AddItineraryStopRequest;
import com.tripflow.itinerary_service.dto.request.CreateItineraryRequest;
import com.tripflow.itinerary_service.dto.response.ItineraryResponse;
import com.tripflow.itinerary_service.dto.response.ItineraryStopResponse;
import com.tripflow.itinerary_service.exception.ResourceNotFoundException;
import com.tripflow.itinerary_service.mapper.ItineraryMapper;
import com.tripflow.itinerary_service.model.Itinerary;
import com.tripflow.itinerary_service.model.ItineraryStop;
import com.tripflow.itinerary_service.repository.ItineraryRepository;
import com.tripflow.itinerary_service.repository.ItineraryStopRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

// Service che contiene la logica applicativa relativa agli itinerari e alle tappe.
// L'ownerId arriva sempre dal token JWT (subject) e non dai parametri client:
// ogni accesso a un itinerario è protetto da un check di proprietà (403 se non owner).
@Service
public class ItineraryService {

    // Repository per la gestione CRUD degli itinerari
    private final ItineraryRepository itineraryRepository;

    // Repository per la gestione CRUD delle tappe
    private final ItineraryStopRepository itineraryStopRepository;

    // Mapper per conversione DTO <-> Entity
    private final ItineraryMapper itineraryMapper;

    // Dependency Injection di repository e mapper tramite costruttore
    public ItineraryService(
            ItineraryRepository itineraryRepository,
            ItineraryStopRepository itineraryStopRepository,
            ItineraryMapper itineraryMapper
    ) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryStopRepository = itineraryStopRepository;
        this.itineraryMapper = itineraryMapper;
    }

    // Crea un nuovo itinerario e lo salva nel database
    public ItineraryResponse createItinerary(CreateItineraryRequest request, UUID ownerId) {
        Itinerary itinerary = itineraryMapper.toEntity(request, ownerId);
        Itinerary savedItinerary = itineraryRepository.save(itinerary);

        return itineraryMapper.toResponse(savedItinerary);
    }

    // Recupera un itinerario tramite ID, solo se appartiene all'owner autenticato
    public ItineraryResponse getItineraryById(Long itineraryId, UUID ownerId) {
        Itinerary itinerary = findOwnedItinerary(itineraryId, ownerId);

        return itineraryMapper.toResponse(itinerary);
    }

    // Recupera tutti gli itinerari appartenenti a un owner
    public List<ItineraryResponse> getItinerariesByOwner(UUID ownerId) {
        return itineraryRepository.findByOwnerId(ownerId)
                .stream()
                .map(itineraryMapper::toResponse)
                .toList();
    }

    // Aggiunge una nuova tappa a un itinerario esistente, solo se appartiene all'owner autenticato
    public ItineraryStopResponse addStopToItinerary(
            Long itineraryId,
            AddItineraryStopRequest request,
            UUID ownerId
    ) {
        Itinerary itinerary = findOwnedItinerary(itineraryId, ownerId);

        ItineraryStop stop = itineraryMapper.toStopEntity(request, itinerary);
        ItineraryStop savedStop = itineraryStopRepository.save(stop);

        return itineraryMapper.toStopResponse(savedStop);
    }

    // Elimina un itinerario dal database, solo se appartiene all'owner autenticato
    public void deleteItinerary(Long itineraryId, UUID ownerId) {
        Itinerary itinerary = findOwnedItinerary(itineraryId, ownerId);

        itineraryRepository.delete(itinerary);
    }

    // Carica l'itinerario e verifica la proprietà: 404 se non esiste, 403 se di un altro owner
    private Itinerary findOwnedItinerary(Long itineraryId, UUID ownerId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found with id: " + itineraryId));

        if (!itinerary.getOwnerId().equals(ownerId)) {
            throw new AccessDeniedException("Itinerary not accessible");
        }
        return itinerary;
    }
}
