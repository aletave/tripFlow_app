package com.tripflow.itinerary_service.service;

import com.tripflow.itinerary_service.client.UserAuthClient;
import com.tripflow.itinerary_service.dto.request.AddItineraryStopRequest;
import com.tripflow.itinerary_service.dto.request.CreateItineraryRequest;
import com.tripflow.itinerary_service.dto.request.ShareItineraryRequest;
import com.tripflow.itinerary_service.dto.request.UpdateItineraryRequest;
import com.tripflow.itinerary_service.dto.request.UpdateItineraryStopRequest;
import com.tripflow.itinerary_service.dto.request.UpdateVisibilityRequest;
import com.tripflow.itinerary_service.dto.response.ItineraryResponse;
import com.tripflow.itinerary_service.dto.response.ItineraryShareResponse;
import com.tripflow.itinerary_service.dto.response.ItineraryStopResponse;
import com.tripflow.itinerary_service.dto.response.PublicItineraryResponse;
import com.tripflow.itinerary_service.exception.ResourceNotFoundException;
import com.tripflow.itinerary_service.mapper.ItineraryMapper;
import com.tripflow.itinerary_service.mapper.ItineraryShareMapper;
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
import org.springframework.transaction.support.TransactionTemplate;

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
    private final ItineraryShareMapper itineraryShareMapper;
    private final UserAuthClient userAuthClient;
    private final TransactionTemplate transactionTemplate;

    public ItineraryService(
            ItineraryRepository itineraryRepository,
            ItineraryStopRepository itineraryStopRepository,
            ItineraryShareRepository itineraryShareRepository,
            ItineraryMapper itineraryMapper,
            ItineraryShareMapper itineraryShareMapper,
            UserAuthClient userAuthClient,
            TransactionTemplate transactionTemplate
    ) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryStopRepository = itineraryStopRepository;
        this.itineraryShareRepository = itineraryShareRepository;
        this.itineraryMapper = itineraryMapper;
        this.itineraryShareMapper = itineraryShareMapper;
        this.userAuthClient = userAuthClient;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public ItineraryResponse createItinerary(CreateItineraryRequest request, UUID ownerId) {
        Visibility visibility = resolveVisibility(request.visibility(), request.isPublic());
        Itinerary itinerary = itineraryMapper.toEntity(request, ownerId, visibility);

        if (visibility == Visibility.PUBLIC) {
            itinerary.setPublicToken(UUID.randomUUID());
        }

        Itinerary savedItinerary = itineraryRepository.save(itinerary);
        return toResponse(savedItinerary, ownerId);
    }

    @Transactional(readOnly = true)
    public ItineraryResponse getItineraryById(Long itineraryId, UUID userId) {
        Itinerary itinerary = findItineraryWithReadAccess(itineraryId, userId);
        return toResponse(itinerary, userId);
    }

    @Transactional(readOnly = true)
    public PublicItineraryResponse getPublicItinerary(UUID publicToken) {
        Itinerary itinerary = itineraryRepository
                .findByPublicTokenAndVisibility(publicToken, Visibility.PUBLIC)
                .orElseThrow(() -> new ResourceNotFoundException("Public itinerary not found"));
        return itineraryMapper.toPublicResponse(itinerary);
    }

    @Transactional(readOnly = true)
    public List<ItineraryResponse> getItinerariesByOwner(UUID ownerId) {
        // L'owner puo' sempre modificare i propri itinerari: canEdit = true senza query aggiuntive.
        return itineraryRepository.findByOwnerId(ownerId)
                .stream()
                .map(i -> itineraryMapper.toResponse(i, true))
                .toList();
    }

    // Restituisce la lista globale degli itinerari accessibili all'utente:
    // quelli posseduti (owner_id) piu' quelli condivisi con lui (itinerary_shares).
    // Un itinerario condiviso resta in lista qualunque sia la sua visibilita' (SHARED
    // o PUBLIC): l'unico caso escluso e' PRIVATE, dove eventuali share sono stantie.
    // canEdit e' calcolato dalle share gia' caricate, senza una query per itinerario.
    // La lettura avviene in un'unica transazione (readOnly) per evitare
    // LazyInitializationException durante il mapping delle tappe.
    @Transactional(readOnly = true)
    public List<ItineraryResponse> getAllAccessibleItineraries(UUID userId) {
        Map<Long, ItineraryResponse> accessible = new LinkedHashMap<>();

        itineraryRepository.findByOwnerId(userId)
                .forEach(itinerary -> accessible.put(itinerary.getId(), itineraryMapper.toResponse(itinerary, true)));

        Map<Long, Boolean> canEditBySharedId = new LinkedHashMap<>();
        itineraryShareRepository.findBySharedWithUserId(userId)
                .forEach(share -> canEditBySharedId.merge(share.getItineraryId(), share.isCanEdit(), Boolean::logicalOr));

        if (!canEditBySharedId.isEmpty()) {
            itineraryRepository.findAllById(List.copyOf(canEditBySharedId.keySet()))
                    .stream()
                    .filter(itinerary -> itinerary.getVisibility() != Visibility.PRIVATE)
                    .forEach(itinerary -> accessible.putIfAbsent(
                            itinerary.getId(),
                            itineraryMapper.toResponse(itinerary, canEditBySharedId.getOrDefault(itinerary.getId(), false))
                    ));
        }

        return List.copyOf(accessible.values());
    }

    @Transactional
    public ItineraryStopResponse addStopToItinerary(Long itineraryId, AddItineraryStopRequest request, UUID userId) {
        Itinerary itinerary = findItineraryWithWriteAccess(itineraryId, userId);
        validateStopReferences(request.stopType(), request.viaggioId(), request.attivitaId());
        validateStopWithinItinerary(itinerary, request.startDatetime(), request.endDatetime());
        ItineraryStop stop = itineraryMapper.toStopEntity(request, itinerary);
        // Se la posizione richiesta e' gia' occupata la tappa viene accodata in fondo
        // (ultima posizione + 1) invece di rispondere 409: l'app, quando l'utente non
        // sceglie una posizione, invia il numero del giorno, che per due tappe nello
        // stesso giorno coincide. La response riporta lo stopOrder effettivo.
        stop.setStopOrder(resolveStopOrderForNewStop(itinerary, request.stopOrder()));
        // Si mantiene allineata la collection gestita (cascade ALL / orphanRemoval).
        itinerary.getStops().add(stop);
        ItineraryStop savedStop = itineraryStopRepository.save(stop);
        return itineraryMapper.toStopResponse(savedStop);
    }

    @Transactional
    public ItineraryStopResponse updateStop(Long itineraryId, Long stopId, UpdateItineraryStopRequest request, UUID userId) {
        Itinerary itinerary = findItineraryWithWriteAccess(itineraryId, userId);

        ItineraryStop stop = itineraryStopRepository.findById(stopId)
                .orElseThrow(() -> new ResourceNotFoundException("Stop not found with id: " + stopId));

        if (!stop.getItinerary().getId().equals(itineraryId)) {
            throw new ResourceNotFoundException("Stop not found in itinerary " + itineraryId);
        }

        itineraryMapper.updateStopEntity(request, stop);
        validateStopReferences(stop.getStopType(), stop.getViaggioId(), stop.getAttivitaId());
        validateStopWithinItinerary(itinerary, stop.getStartDatetime(), stop.getEndDatetime());
        validateStopOrderAvailable(itinerary, stop.getStopOrder(), stop.getId());
        if (stop.getStartDatetime() != null && stop.getEndDatetime() != null
                && stop.getEndDatetime().isBefore(stop.getStartDatetime())) {
            // Caso di update parziale: la nuova fine puo' precedere l'inizio gia' salvato.
            throw new IllegalArgumentException("La data di fine della tappa non puo' essere precedente alla data di inizio");
        }
        ItineraryStop updatedStop = itineraryStopRepository.save(stop);
        return itineraryMapper.toStopResponse(updatedStop);
    }

    @Transactional
    public void deleteStop(Long itineraryId, Long stopId, UUID userId) {
        Itinerary itinerary = findItineraryWithWriteAccess(itineraryId, userId);

        // findById carica le tappe con @EntityGraph("stops"): la tappa e' quindi
        // gestita dalla collection itinerary.stops (cascade ALL). Se ci si limita a
        // itineraryStopRepository.delete(stop), al flush Hibernate la ri-persiste
        // tramite la cascade e la riga resta nel DB. La tappa va quindi cercata e
        // rimossa dalla collection stessa: orphanRemoval genera la DELETE.
        ItineraryStop stop = itinerary.getStops()
                .stream()
                .filter(s -> stopId.equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tappa " + stopId + " non trovata nell'itinerario " + itineraryId));

        itinerary.getStops().remove(stop);
        itineraryStopRepository.delete(stop);
    }

    @Transactional
    public void deleteItinerary(Long itineraryId, UUID ownerId) {
        Itinerary itinerary = findOwnedItinerary(itineraryId, ownerId);
        itineraryRepository.delete(itinerary);
    }

    @Transactional
    public ItineraryResponse updateItinerary(Long itineraryId, UpdateItineraryRequest request, UUID userId) {
        Itinerary itinerary = findItineraryWithWriteAccess(itineraryId, userId);
        itineraryMapper.updateEntity(request, itinerary);
        // Le tappe gia' presenti devono restare nel nuovo range dell'itinerario.
        for (ItineraryStop stop : itinerary.getStops()) {
            try {
                validateStopWithinItinerary(itinerary, stop.getStartDatetime(), stop.getEndDatetime());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Le nuove date escludono la tappa " + stop.getId()
                        + " (stopOrder " + stop.getStopOrder() + "): " + e.getMessage(), e);
            }
        }
        Itinerary updated = itineraryRepository.save(itinerary);
        return toResponse(updated, userId);
    }

    @Transactional
    public ItineraryResponse updateVisibility(Long itineraryId, UpdateVisibilityRequest request, UUID userId) {
        if (request.visibility() == null && request.isPublic() == null) {
            throw new IllegalArgumentException("Almeno uno tra visibility e isPublic deve essere specificato");
        }

        Itinerary itinerary = findOwnedItinerary(itineraryId, userId);

        Visibility visibility = resolveVisibility(request.visibility(), request.isPublic());
        itinerary.setVisibility(visibility);

        if (visibility == Visibility.PUBLIC) {
            if (itinerary.getPublicToken() == null) {
                itinerary.setPublicToken(UUID.randomUUID());
            }
        } else {
            itinerary.setPublicToken(null);
        }

        if (visibility == Visibility.PRIVATE) {
            itineraryShareRepository.deleteByItineraryId(itineraryId);
        }

        Itinerary updated = itineraryRepository.save(itinerary);
        return toResponse(updated, userId);
    }

    // Il metodo NON e' @Transactional: la verifica remota su user-auth-service
    // deve avvenire senza una transazione (e quindi una connessione HikariCP)
    // aperta. Il repository findById apre una breve transazione read-only che
    // viene chiusa prima della chiamata HTTP; solo il salvataggio finale viene
    // eseguito dentro TransactionTemplate.
    public ItineraryShareResponse shareItinerary(Long itineraryId, ShareItineraryRequest request, UUID ownerId) {
        if (ownerId.equals(request.sharedWithUserId())) {
            throw new IllegalArgumentException("Non puoi condividere un itinerario con te stesso");
        }

        // Fase 1: verifica di ownership in una transazione read-only breve.
        // Serve anche a evitare che un non-owner possa far partire chiamate
        // remote verso user-auth-service.
        findOwnedItinerary(itineraryId, ownerId);

        // Fase 2: chiamata esterna fuori da qualsiasi transazione DB.
        if (!userAuthClient.userExists(request.sharedWithUserId())) {
            throw new ResourceNotFoundException("Utente non trovato: " + request.sharedWithUserId());
        }

        // Fase 3: persistenza in una transazione breve e separata.
        return transactionTemplate.execute(status -> persistShare(itineraryId, request, ownerId));
    }

    private ItineraryShareResponse persistShare(Long itineraryId, ShareItineraryRequest request, UUID ownerId) {
        Itinerary itinerary = findOwnedItinerary(itineraryId, ownerId);

        if (itineraryShareRepository.existsByItineraryIdAndSharedWithUserId(itineraryId, request.sharedWithUserId())) {
            throw new IllegalArgumentException("Itinerary already shared with this user");
        }

        ItineraryShare share = itineraryShareMapper.toEntity(request, itineraryId);
        ItineraryShare savedShare = itineraryShareRepository.save(share);

        if (itinerary.getVisibility() == Visibility.PRIVATE) {
            itinerary.setVisibility(Visibility.SHARED);
            itineraryRepository.save(itinerary);
        }

        return itineraryShareMapper.toResponse(savedShare);
    }

    @Transactional
    public void removeShare(Long itineraryId, UUID sharedWithUserId, UUID ownerId) {
        Itinerary itinerary = findOwnedItinerary(itineraryId, ownerId);

        if (!itineraryShareRepository.existsByItineraryIdAndSharedWithUserId(itineraryId, sharedWithUserId)) {
            throw new ResourceNotFoundException("Share not found for itinerary " + itineraryId + " and user " + sharedWithUserId);
        }
        itineraryShareRepository.deleteByItineraryIdAndSharedWithUserId(itineraryId, sharedWithUserId);

        // Simmetrico a persistShare (PRIVATE -> SHARED alla prima condivisione):
        // rimossa l'ultima condivisione, un itinerario SHARED torna PRIVATE.
        // Un itinerario PUBLIC resta PUBLIC.
        if (itinerary.getVisibility() == Visibility.SHARED
                && !itineraryShareRepository.existsByItineraryId(itineraryId)) {
            itinerary.setVisibility(Visibility.PRIVATE);
            itineraryRepository.save(itinerary);
        }
    }

    @Transactional(readOnly = true)
    public List<ItineraryShareResponse> getShares(Long itineraryId, UUID ownerId) {
        findOwnedItinerary(itineraryId, ownerId);

        return itineraryShareRepository.findByItineraryId(itineraryId)
                .stream()
                .map(itineraryShareMapper::toResponse)
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

    private static final java.util.Set<String> STOP_TYPES = java.util.Set.of("TRIP", "ACTIVITY", "CUSTOM");

    // Controlla la coerenza tipo/riferimenti della tappa prima di arrivare ai vincoli del DB (400 invece di 409).
    private void validateStopReferences(String stopType, UUID viaggioId, UUID attivitaId) {
        if (stopType == null || !STOP_TYPES.contains(stopType)) {
            throw new IllegalArgumentException(
                    "stopType non valido: " + stopType + ". Valori ammessi: TRIP, ACTIVITY, CUSTOM (maiuscolo)");
        }
        switch (stopType) {
            case "TRIP" -> {
                if (viaggioId == null) {
                    throw new IllegalArgumentException("Una tappa di tipo TRIP richiede viaggioId");
                }
                if (attivitaId != null) {
                    throw new IllegalArgumentException("Una tappa di tipo TRIP non puo avere attivitaId");
                }
            }
            case "ACTIVITY" -> {
                if (attivitaId == null) {
                    throw new IllegalArgumentException("Una tappa di tipo ACTIVITY richiede attivitaId");
                }
                if (viaggioId != null) {
                    throw new IllegalArgumentException("Una tappa di tipo ACTIVITY non puo avere viaggioId");
                }
            }
            default -> {
                if (viaggioId != null || attivitaId != null) {
                    throw new IllegalArgumentException("Una tappa di tipo CUSTOM non puo avere viaggioId o attivitaId");
                }
            }
        }
    }

    private int resolveStopOrderForNewStop(Itinerary itinerary, Integer requested) {
        boolean taken = itinerary.getStops().stream()
                .anyMatch(s -> requested.equals(s.getStopOrder()));
        if (!taken) {
            return requested;
        }
        return itinerary.getStops().stream()
                .map(ItineraryStop::getStopOrder)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compare)
                .orElse(0) + 1;
    }

    // Anticipa il vincolo unique_itinerary_stop_order con un messaggio esplicito (409).
    private void validateStopOrderAvailable(Itinerary itinerary, Integer stopOrder, Long excludedStopId) {
        if (stopOrder == null) {
            return;
        }
        boolean taken = itinerary.getStops().stream()
                .anyMatch(s -> stopOrder.equals(s.getStopOrder()) && !java.util.Objects.equals(s.getId(), excludedStopId));
        if (taken) {
            throw new IllegalStateException(
                    "Esiste gia' una tappa con stopOrder " + stopOrder + " in questo itinerario");
        }
    }

    // Le date della tappa devono ricadere nel range dell'itinerario (se definito).
    private void validateStopWithinItinerary(Itinerary itinerary, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        java.time.LocalDate from = itinerary.getStartDate();
        java.time.LocalDate to = itinerary.getEndDate();
        for (java.time.LocalDateTime value : new java.time.LocalDateTime[]{start, end}) {
            if (value == null) {
                continue;
            }
            if (from != null && value.toLocalDate().isBefore(from)) {
                throw new IllegalArgumentException(
                        "Le date della tappa non possono essere precedenti all'inizio dell'itinerario (" + from + ")");
            }
            if (to != null && value.toLocalDate().isAfter(to)) {
                throw new IllegalArgumentException(
                        "Le date della tappa non possono essere successive alla fine dell'itinerario (" + to + ")");
            }
        }
    }

    private ItineraryResponse toResponse(Itinerary itinerary, UUID userId) {
        return itineraryMapper.toResponse(itinerary, hasWriteAccess(itinerary, userId));
    }

    private boolean hasReadAccess(Itinerary itinerary, UUID userId) {
        if (itinerary.getOwnerId().equals(userId)) {
            return true;
        }
        if (itinerary.getVisibility() == Visibility.PRIVATE) {
            return false;
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
        if (itinerary.getVisibility() == Visibility.PRIVATE) {
            return false;
        }
        Optional<ItineraryShare> share = itineraryShareRepository
                .findByItineraryIdAndSharedWithUserId(itinerary.getId(), userId);
        return share.isPresent() && share.get().isCanEdit();
    }

    private Visibility resolveVisibility(Visibility visibility, Boolean isPublic) {
        if (visibility != null) {
            return visibility;
        }
        if (isPublic != null) {
            return isPublic ? Visibility.PUBLIC : Visibility.PRIVATE;
        }
        return Visibility.PRIVATE;
    }
}
