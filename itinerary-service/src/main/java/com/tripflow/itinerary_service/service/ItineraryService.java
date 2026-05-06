package com.tripflow.itinerary_service.service;

import com.tripflow.itinerary_service.dto.request.AddItineraryStopRequest;
import com.tripflow.itinerary_service.dto.request.CreateItineraryRequest;
import com.tripflow.itinerary_service.dto.response.ItineraryResponse;
import com.tripflow.itinerary_service.dto.response.ItineraryStopResponse;
import com.tripflow.itinerary_service.mapper.ItineraryMapper;
import com.tripflow.itinerary_service.model.Itinerary;
import com.tripflow.itinerary_service.model.ItineraryStop;
import com.tripflow.itinerary_service.repository.ItineraryRepository;
import com.tripflow.itinerary_service.repository.ItineraryStopRepository;
import org.springframework.stereotype.Service;

import java.util.List;

// Service che contiene la logica applicativa relativa agli itinerari e alle tappe
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
    public ItineraryResponse createItinerary(CreateItineraryRequest request, Long ownerId) {
        Itinerary itinerary = itineraryMapper.toEntity(request, ownerId);
        Itinerary savedItinerary = itineraryRepository.save(itinerary);

        return itineraryMapper.toResponse(savedItinerary);
    }

    // Recupera un itinerario tramite ID
    public ItineraryResponse getItineraryById(Long itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found"));

        return itineraryMapper.toResponse(itinerary);
    }

    // Recupera tutti gli itinerari appartenenti a un owner
    public List<ItineraryResponse> getItinerariesByOwner(Long ownerId) {
        return itineraryRepository.findByOwnerId(ownerId)
                .stream()
                .map(itineraryMapper::toResponse)
                .toList();
    }

    // Aggiunge una nuova tappa a un itinerario esistente
    public ItineraryStopResponse addStopToItinerary(
            Long itineraryId,
            AddItineraryStopRequest request
    ) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found"));

        ItineraryStop stop = itineraryMapper.toStopEntity(request, itinerary);
        ItineraryStop savedStop = itineraryStopRepository.save(stop);

        return itineraryMapper.toStopResponse(savedStop);
    }

    // Elimina un itinerario dal database
    public void deleteItinerary(Long itineraryId) {
        if (!itineraryRepository.existsById(itineraryId)) {
            throw new RuntimeException("Itinerary not found");
        }

        itineraryRepository.deleteById(itineraryId);
    }
}