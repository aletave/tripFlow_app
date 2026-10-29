package com.tripflow.itinerary_service.controller;

import com.tripflow.itinerary_service.configSecurity.SecurityUtils;
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
import com.tripflow.itinerary_service.service.ItineraryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoint REST di itinerari, tappe e condivisioni ({@code /api/itineraries}).
 *
 * <p>L'utente e' sempre ricavato dal JWT tramite {@link SecurityUtils#ownerId()}.
 * I controlli di accesso sono nel service:
 * <ul>
 *   <li>lettura: proprietario, utente con condivisione o itinerario {@code PUBLIC};</li>
 *   <li>tappe e modifica dati: proprietario o condivisione con {@code canEdit};</li>
 *   <li>visibilita', condivisioni, eliminazione: solo il proprietario;</li>
 *   <li>{@code GET /public/{token}}: senza autenticazione.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/itineraries")
public class ItineraryController {

    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    /** Crea un itinerario dell'utente corrente (201). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItineraryResponse createItinerary(
            @Valid @RequestBody CreateItineraryRequest request
    ) {
        return itineraryService.createItinerary(request, SecurityUtils.ownerId());
    }

    /** Itinerari propri e condivisi con l'utente: e' la lista mostrata nell'app. */
    @GetMapping
    public List<ItineraryResponse> getAllItineraries() {
        return itineraryService.getAllAccessibleItineraries(SecurityUtils.ownerId());
    }

    /** Dettaglio di un itinerario con le sue tappe (403 se non accessibile). */
    @GetMapping("/{id}")
    public ItineraryResponse getItineraryById(@PathVariable Long id) {
        return itineraryService.getItineraryById(id, SecurityUtils.ownerId());
    }

    /** Vista pubblica di un itinerario {@code PUBLIC}, senza autenticazione. */
    @GetMapping("/public/{token}")
    public PublicItineraryResponse getPublicItinerary(@PathVariable UUID token) {
        return itineraryService.getPublicItinerary(token);
    }

    /** Aggiunge una tappa (201). Viaggi e attivita' vengono verificati sul catalogo. */
    @PostMapping("/{id}/stops")
    @ResponseStatus(HttpStatus.CREATED)
    public ItineraryStopResponse addStopToItinerary(
            @PathVariable Long id,
            @Valid @RequestBody AddItineraryStopRequest request
    ) {
        return itineraryService.addStopToItinerary(id, request, SecurityUtils.ownerId());
    }

    /** Modifica una tappa: vengono aggiornati solo i campi presenti nel body. */
    @PutMapping("/{id}/stops/{stopId}")
    public ItineraryStopResponse updateStop(
            @PathVariable Long id,
            @PathVariable Long stopId,
            @Valid @RequestBody UpdateItineraryStopRequest request
    ) {
        return itineraryService.updateStop(id, stopId, request, SecurityUtils.ownerId());
    }

    /** Elimina una tappa. */
    @DeleteMapping("/{id}/stops/{stopId}")
    public void deleteStop(
            @PathVariable Long id,
            @PathVariable Long stopId
    ) {
        itineraryService.deleteStop(id, stopId, SecurityUtils.ownerId());
    }

    /** Cambia la visibilita' (PRIVATE, SHARED, PUBLIC). Solo il proprietario. */
    @PatchMapping("/{id}/visibility")
    public ItineraryResponse updateVisibility(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVisibilityRequest request
    ) {
        return itineraryService.updateVisibility(id, request, SecurityUtils.ownerId());
    }

    /** Aggiorna titolo, descrizione e date dell'itinerario. */
    @PutMapping("/{id}")
    public ItineraryResponse updateItinerary(
            @PathVariable Long id,
            @Valid @RequestBody UpdateItineraryRequest request
    ) {
        return itineraryService.updateItinerary(id, request, SecurityUtils.ownerId());
    }

    /** Elimina l'itinerario con tutte le tappe. Solo il proprietario. */
    @DeleteMapping("/{id}")
    public void deleteItinerary(@PathVariable Long id) {
        itineraryService.deleteItinerary(id, SecurityUtils.ownerId());
    }

    /** Condivide l'itinerario con un utente (201). Solo il proprietario. */
    @PostMapping("/{id}/shares")
    @ResponseStatus(HttpStatus.CREATED)
    public ItineraryShareResponse shareItinerary(
            @PathVariable Long id,
            @Valid @RequestBody ShareItineraryRequest request
    ) {
        return itineraryService.shareItinerary(id, request, SecurityUtils.ownerId());
    }

    /** Revoca la condivisione con un utente. Solo il proprietario. */
    @DeleteMapping("/{id}/shares/{sharedWithUserId}")
    public void removeShare(
            @PathVariable Long id,
            @PathVariable UUID sharedWithUserId
    ) {
        itineraryService.removeShare(id, sharedWithUserId, SecurityUtils.ownerId());
    }

    /** Elenca le condivisioni dell'itinerario. Solo il proprietario. */
    @GetMapping("/{id}/shares")
    public List<ItineraryShareResponse> getShares(@PathVariable Long id) {
        return itineraryService.getShares(id, SecurityUtils.ownerId());
    }
}
