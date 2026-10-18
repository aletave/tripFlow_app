package com.tripflow.itinerary_service.service;

import com.tripflow.itinerary_service.dto.request.AddItineraryStopRequest;
import com.tripflow.itinerary_service.dto.request.CreateItineraryRequest;
import com.tripflow.itinerary_service.dto.request.ShareItineraryRequest;
import com.tripflow.itinerary_service.dto.request.UpdateItineraryStopRequest;
import com.tripflow.itinerary_service.dto.request.UpdateVisibilityRequest;
import com.tripflow.itinerary_service.dto.response.ItineraryResponse;
import com.tripflow.itinerary_service.dto.response.ItineraryShareResponse;
import com.tripflow.itinerary_service.dto.response.ItineraryStopResponse;
import com.tripflow.itinerary_service.exception.ResourceNotFoundException;
import com.tripflow.itinerary_service.mapper.ItineraryMapper;
import com.tripflow.itinerary_service.model.Itinerary;
import com.tripflow.itinerary_service.model.ItineraryShare;
import com.tripflow.itinerary_service.model.ItineraryStop;
import com.tripflow.itinerary_service.model.Visibility;
import com.tripflow.itinerary_service.repository.ItineraryRepository;
import com.tripflow.itinerary_service.repository.ItineraryShareRepository;
import com.tripflow.itinerary_service.repository.ItineraryStopRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Service che contiene la logica applicativa relativa agli itinerari e alle tappe.
// Livelli di autorizzazione:
// 1. READ: owner, shared user (via itinerary_shares), or PUBLIC visibility
// 2. WRITE (stops/update): owner or shared user with can_edit = true
// 3. MANAGE/DELETE: only owner
@Service
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryStopRepository itineraryStopRepository;
    private final ItineraryShareRepository itineraryShareRepository;
    private final ItineraryMapper itineraryMapper;

    public ItineraryService(
            ItineraryRepository itineraryRepository,
            ItineraryStopRepository itineraryStopRepository,
            ItineraryShareRepository itineraryShareRepository,
            ItineraryMapper itineraryMapper
    ) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryStopRepository = itineraryStopRepository;
        this.itineraryShareRepository = itineraryShareRepository;
        this.itineraryMapper = itineraryMapper;
    }

    public ItineraryResponse createItinerary(CreateItineraryRequest request, UUID ownerId) {
        Itinerary itinerary = itineraryMapper.toEntity(request, ownerId);
        Itinerary savedItinerary = itineraryRepository.save(itinerary);
        return itineraryMapper.toResponse(savedItinerary);
    }

    public ItineraryResponse getItineraryById(Long itineraryId, UUID userId) {
        Itinerary itinerary = findItineraryWithReadAccess(itineraryId, userId);
        return itineraryMapper.toResponse(itinerary);
    }

    public ItineraryResponse getPublicItinerary(Long itineraryId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found with id: " + itineraryId));

        if (itinerary.getVisibility() != Visibility.PUBLIC) {
            throw new AccessDeniedException("Itinerary is not public");
        }
        return itineraryMapper.toResponse(itinerary);
    }

    public List<ItineraryResponse> getItinerariesByOwner(UUID ownerId) {
        return itineraryRepository.findByOwnerId(ownerId)
                .stream()
                .map(itineraryMapper::toResponse)
                .toList();
    }

    // Restituisce la lista globale degli itinerari accessibili all'utente:
    // quelli posseduti (owner_id) piu' quelli condivisi con lui (itinerary_shares).
    // La lettura avviene in un'unica transazione (readOnly) per evitare
    // LazyInitializationException durante il mapping delle tappe.
    @Transactional(readOnly = true)
    public List<ItineraryResponse> getAllAccessibleItineraries(UUID userId) {
        Map<Long, Itinerary> accessible = new LinkedHashMap<>();

        itineraryRepository.findByOwnerId(userId)
                .forEach(itinerary -> accessible.put(itinerary.getId(), itinerary));

        List<Long> sharedIds = itineraryShareRepository.findBySharedWithUserId(userId)
                .stream()
                .map(ItineraryShare::getItineraryId)
                .toList();

        if (!sharedIds.isEmpty()) {
            itineraryRepository.findAllById(sharedIds)
                    .forEach(itinerary -> accessible.putIfAbsent(itinerary.getId(), itinerary));
        }

        return accessible.values()
                .stream()
                .map(itineraryMapper::toResponse)
                .toList();
    }

    public ItineraryStopResponse addStopToItinerary(Long itineraryId, AddItineraryStopRequest request, UUID userId) {
        Itinerary itinerary = findItineraryWithWriteAccess(itineraryId, userId);
        ItineraryStop stop = itineraryMapper.toStopEntity(request, itinerary);
        ItineraryStop savedStop = itineraryStopRepository.save(stop);
        return itineraryMapper.toStopResponse(savedStop);
    }

    public ItineraryStopResponse updateStop(Long itineraryId, Long stopId, UpdateItineraryStopRequest request, UUID userId) {
        Itinerary itinerary = findItineraryWithWriteAccess(itineraryId, userId);

        ItineraryStop stop = itineraryStopRepository.findById(stopId)
                .orElseThrow(() -> new ResourceNotFoundException("Stop not found with id: " + stopId));

        if (!stop.getItinerary().getId().equals(itineraryId)) {
            throw new ResourceNotFoundException("Stop not found in itinerary " + itineraryId);
        }

        itineraryMapper.updateStopEntity(request, stop);
        ItineraryStop updatedStop = itineraryStopRepository.save(stop);
        return itineraryMapper.toStopResponse(updatedStop);
    }

    public void deleteStop(Long itineraryId, Long stopId, UUID userId) {
        Itinerary itinerary = findItineraryWithWriteAccess(itineraryId, userId);

        ItineraryStop stop = itineraryStopRepository.findById(stopId)
                .orElseThrow(() -> new ResourceNotFoundException("Stop not found with id: " + stopId));

        if (!stop.getItinerary().getId().equals(itineraryId)) {
            throw new ResourceNotFoundException("Stop not found in itinerary " + itineraryId);
        }

        itineraryStopRepository.delete(stop);
    }

    public void deleteItinerary(Long itineraryId, UUID ownerId) {
        Itinerary itinerary = findOwnedItinerary(itineraryId, ownerId);
        itineraryRepository.delete(itinerary);
    }

    public ItineraryResponse updateItinerary(Long itineraryId, CreateItineraryRequest request, UUID userId) {
        Itinerary itinerary = findItineraryWithWriteAccess(itineraryId, userId);
        itineraryMapper.updateEntity(request, itinerary);
        Itinerary updated = itineraryRepository.save(itinerary);
        return itineraryMapper.toResponse(updated);
    }

    public ItineraryResponse updateVisibility(Long itineraryId, UpdateVisibilityRequest request, UUID userId) {
        Itinerary itinerary = findOwnedItinerary(itineraryId, userId);
        itinerary.setVisibility(request.resolvedVisibility());
        Itinerary updated = itineraryRepository.save(itinerary);
        return itineraryMapper.toResponse(updated);
    }

    public ItineraryShareResponse shareItinerary(Long itineraryId, ShareItineraryRequest request, UUID ownerId) {
        Itinerary itinerary = findOwnedItinerary(itineraryId, ownerId);

        if (itineraryShareRepository.existsByItineraryIdAndSharedWithUserId(itineraryId, request.sharedWithUserId())) {
            throw new IllegalArgumentException("Itinerary already shared with this user");
        }

        ItineraryShare share = new ItineraryShare();
        share.setItineraryId(itineraryId);
        share.setSharedWithUserId(request.sharedWithUserId());
        share.setCanEdit(request.canEdit());

        ItineraryShare savedShare = itineraryShareRepository.save(share);
        return toShareResponse(savedShare);
    }

    public void removeShare(Long itineraryId, UUID sharedWithUserId, UUID ownerId) {
        findOwnedItinerary(itineraryId, ownerId);

        if (!itineraryShareRepository.existsByItineraryIdAndSharedWithUserId(itineraryId, sharedWithUserId)) {
            throw new ResourceNotFoundException("Share not found for itinerary " + itineraryId + " and user " + sharedWithUserId);
        }
        itineraryShareRepository.deleteByItineraryIdAndSharedWithUserId(itineraryId, sharedWithUserId);
    }

    public List<ItineraryShareResponse> getShares(Long itineraryId, UUID ownerId) {
        findOwnedItinerary(itineraryId, ownerId);

        return itineraryShareRepository.findByItineraryId(itineraryId)
                .stream()
                .map(this::toShareResponse)
                .toList();
    }

    private Itinerary findOwnedItinerary(Long itineraryId, UUID ownerId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found with id: " + itineraryId));

        if (!itinerary.getOwnerId().equals(ownerId)) {
            throw new AccessDeniedException("Only the owner can perform this action");
        }
        return itinerary;
    }

    private Itinerary findItineraryWithReadAccess(Long itineraryId, UUID userId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found with id: " + itineraryId));

        if (hasReadAccess(itinerary, userId)) {
            return itinerary;
        }
        throw new AccessDeniedException("Itinerary not accessible");
    }

    private Itinerary findItineraryWithWriteAccess(Long itineraryId, UUID userId) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found with id: " + itineraryId));

        if (hasWriteAccess(itinerary, userId)) {
            return itinerary;
        }
        throw new AccessDeniedException("Not authorized to modify this itinerary");
    }

    private boolean hasReadAccess(Itinerary itinerary, UUID userId) {
        if (itinerary.getOwnerId().equals(userId)) {
            return true;
        }
        if (itinerary.getVisibility() == Visibility.PUBLIC) {
            return true;
        }
        return itineraryShareRepository.findByItineraryIdAndSharedWithUserId(itinerary.getId(), userId)
                .isPresent();
    }

    private boolean hasWriteAccess(Itinerary itinerary, UUID userId) {
        if (itinerary.getOwnerId().equals(userId)) {
            return true;
        }
        Optional<ItineraryShare> share = itineraryShareRepository.findByItineraryIdAndSharedWithUserId(itinerary.getId(), userId);
        return share.isPresent() && share.get().isCanEdit();
    }

    private ItineraryShareResponse toShareResponse(ItineraryShare share) {
        return new ItineraryShareResponse(
                share.getId(),
                share.getItineraryId(),
                share.getSharedWithUserId(),
                share.isCanEdit(),
                share.getCreatedAt()
        );
    }
}
