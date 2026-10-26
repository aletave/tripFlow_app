package com.tripflow.itinerary_service.service;

import com.tripflow.itinerary_service.client.UserAuthClient;
import com.tripflow.itinerary_service.dto.request.CreateItineraryRequest;
import com.tripflow.itinerary_service.dto.request.ShareItineraryRequest;
import com.tripflow.itinerary_service.dto.request.UpdateItineraryRequest;
import com.tripflow.itinerary_service.dto.request.UpdateVisibilityRequest;
import com.tripflow.itinerary_service.dto.response.ItineraryResponse;
import com.tripflow.itinerary_service.dto.response.ItineraryShareResponse;
import com.tripflow.itinerary_service.exception.ResourceNotFoundException;
import com.tripflow.itinerary_service.mapper.ItineraryMapper;
import com.tripflow.itinerary_service.mapper.ItineraryShareMapper;
import com.tripflow.itinerary_service.model.Itinerary;
import com.tripflow.itinerary_service.model.ItineraryShare;
import com.tripflow.itinerary_service.model.Visibility;
import com.tripflow.itinerary_service.repository.ItineraryRepository;
import com.tripflow.itinerary_service.repository.ItineraryShareRepository;
import com.tripflow.itinerary_service.repository.ItineraryStopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItineraryServiceTest {

    @Mock
    private ItineraryRepository itineraryRepository;
    @Mock
    private ItineraryStopRepository itineraryStopRepository;
    @Mock
    private ItineraryShareRepository itineraryShareRepository;
    @Mock
    private UserAuthClient userAuthClient;

    private ItineraryService itineraryService;

    private final UUID userId = UUID.fromString("70cd15d7-3a55-407c-9909-1f575e86cbfe");
    private final UUID otherOwner = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private final UUID sharedUserId = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeEach
    void setUp() {
        itineraryService = new ItineraryService(
                itineraryRepository,
                itineraryStopRepository,
                itineraryShareRepository,
                new ItineraryMapper(),
                new ItineraryShareMapper(),
                userAuthClient
        );
    }

    private Itinerary itinerary(Long id, UUID ownerId, Visibility visibility) {
        Itinerary itinerary = new Itinerary();
        itinerary.setId(id);
        itinerary.setOwnerId(ownerId);
        itinerary.setTitle("Itinerario " + id);
        itinerary.setVisibility(visibility);
        return itinerary;
    }

    private ItineraryShare share(Long itineraryId, UUID sharedWith) {
        ItineraryShare share = new ItineraryShare();
        share.setId(1L);
        share.setItineraryId(itineraryId);
        share.setSharedWithUserId(sharedWith);
        share.setCanEdit(false);
        return share;
    }

    @Test
    void getAllAccessibleItineraries_uniscePossedutiECondivisi() {
        Itinerary owned = itinerary(1L, userId, Visibility.PRIVATE);
        Itinerary shared = itinerary(2L, otherOwner, Visibility.SHARED);

        when(itineraryRepository.findByOwnerId(userId)).thenReturn(List.of(owned));
        when(itineraryShareRepository.findBySharedWithUserId(userId)).thenReturn(List.of(share(2L, userId)));
        when(itineraryRepository.findAllById(List.of(2L))).thenReturn(List.of(shared));

        List<ItineraryResponse> result = itineraryService.getAllAccessibleItineraries(userId);

        assertThat(result).hasSize(2);
        assertThat(result.stream().map(ItineraryResponse::id).toList()).containsExactly(1L, 2L);
    }

    @Test
    void getAllAccessibleItineraries_evitaDuplicati() {
        Itinerary owned = itinerary(1L, userId, Visibility.PRIVATE);

        when(itineraryRepository.findByOwnerId(userId)).thenReturn(List.of(owned));
        when(itineraryShareRepository.findBySharedWithUserId(userId)).thenReturn(List.of(share(1L, userId)));
        when(itineraryRepository.findAllById(List.of(1L))).thenReturn(List.of(owned));

        List<ItineraryResponse> result = itineraryService.getAllAccessibleItineraries(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    @Test
    void createItinerary_isPublicTrue_creaConVisibilitaPubblica() {
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateItineraryRequest request = new CreateItineraryRequest(
                "Solo booleano", "descrizione", null, true, null, null);

        ItineraryResponse response = itineraryService.createItinerary(request, userId);

        assertThat(response.visibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(response.ownerId()).isEqualTo(userId);
    }

    @Test
    void createItinerary_enumHaPrioritaSulBooleano() {
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateItineraryRequest request = new CreateItineraryRequest(
                "Enum esplicito", "descrizione", Visibility.SHARED, true, null, null);

        ItineraryResponse response = itineraryService.createItinerary(request, userId);

        assertThat(response.visibility()).isEqualTo(Visibility.SHARED);
    }

    @Test
    void createItinerary_senzaVisibilita_defaultPrivate() {
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateItineraryRequest request = new CreateItineraryRequest(
                "Senza visibilita'", null, null, null, null, null);

        ItineraryResponse response = itineraryService.createItinerary(request, userId);

        assertThat(response.visibility()).isEqualTo(Visibility.PRIVATE);
    }

    @Test
    void updateVisibility_daIsPublic_rendePubblico() {
        Itinerary itinerary = itinerary(1L, userId, Visibility.PRIVATE);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(itinerary)).thenReturn(itinerary);

        ItineraryResponse response = itineraryService.updateVisibility(
                1L, new UpdateVisibilityRequest(null, true), userId);

        assertThat(response.visibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(response.publicToken()).isNotNull();
        assertThat(itinerary.getPublicToken()).isEqualTo(response.publicToken());
    }

    @Test
    void updateVisibility_enumHaPrioritaSulBooleano() {
        Itinerary itinerary = itinerary(1L, userId, Visibility.PRIVATE);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(itinerary)).thenReturn(itinerary);

        ItineraryResponse response = itineraryService.updateVisibility(
                1L, new UpdateVisibilityRequest(Visibility.SHARED, false), userId);

        assertThat(response.visibility()).isEqualTo(Visibility.SHARED);
        assertThat(response.publicToken()).isNull();
    }

    @Test
    void updateVisibility_senzaVisibilita_defaultPrivate() {
        Itinerary itinerary = itinerary(1L, userId, Visibility.SHARED);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(itinerary)).thenReturn(itinerary);

        ItineraryResponse response = itineraryService.updateVisibility(
                1L, new UpdateVisibilityRequest(null, null), userId);

        assertThat(response.visibility()).isEqualTo(Visibility.PRIVATE);
    }

    @Test
    void updateVisibility_daPubblicoAPrivato_revocaIlToken() {
        UUID token = UUID.fromString("12345678-1234-1234-1234-123456789012");
        Itinerary itinerary = itinerary(1L, userId, Visibility.PUBLIC);
        itinerary.setPublicToken(token);

        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(itinerary)).thenReturn(itinerary);

        ItineraryResponse response = itineraryService.updateVisibility(
                1L, new UpdateVisibilityRequest(Visibility.PRIVATE, null), userId);

        assertThat(response.visibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(response.publicToken()).isNull();
        assertThat(itinerary.getPublicToken()).isNull();
    }

    @Test
    void getPublicItinerary_tokenValido_restituisceItinerario() {
        UUID token = UUID.fromString("12345678-1234-1234-1234-123456789012");
        Itinerary itinerary = itinerary(1L, otherOwner, Visibility.PUBLIC);
        itinerary.setPublicToken(token);

        when(itineraryRepository.findByPublicTokenAndVisibility(token, Visibility.PUBLIC))
                .thenReturn(Optional.of(itinerary));

        ItineraryResponse response = itineraryService.getPublicItinerary(token);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.publicToken()).isEqualTo(token);
    }

    @Test
    void getItineraryById_privateConShareStantio_negaLaLettura() {
        Itinerary itinerary = itinerary(1L, otherOwner, Visibility.PRIVATE);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));

        assertThatThrownBy(() -> itineraryService.getItineraryById(1L, sharedUserId))
                .isInstanceOf(AccessDeniedException.class);

        verify(itineraryShareRepository, never())
                .findByItineraryIdAndSharedWithUserId(1L, sharedUserId);
    }

    @Test
    void updateItinerary_privateConShareStantio_negaLaScrittura() {
        Itinerary itinerary = itinerary(1L, otherOwner, Visibility.PRIVATE);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));

        UpdateItineraryRequest request = new UpdateItineraryRequest(
                "Titolo modificato", "descrizione", null, null);

        assertThatThrownBy(() -> itineraryService.updateItinerary(1L, request, sharedUserId))
                .isInstanceOf(AccessDeniedException.class);

        verify(itineraryRepository, never()).save(any(Itinerary.class));
    }

    @Test
    void updateItinerary_editorCondivisoNonPuoCambiareVisibilita() {
        Itinerary itinerary = itinerary(1L, otherOwner, Visibility.SHARED);
        ItineraryShare editableShare = share(1L, userId);
        editableShare.setCanEdit(true);

        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(itineraryShareRepository.findByItineraryIdAndSharedWithUserId(1L, userId))
                .thenReturn(Optional.of(editableShare));
        when(itineraryRepository.save(itinerary)).thenReturn(itinerary);

        itineraryService.updateItinerary(1L,
                new UpdateItineraryRequest("Nuovo titolo", "nuova descrizione", null, null),
                userId);

        assertThat(itinerary.getTitle()).isEqualTo("Nuovo titolo");
        assertThat(itinerary.getVisibility()).isEqualTo(Visibility.SHARED);
    }

    @Test
    void getAllAccessibleItineraries_escludeShareStantii() {
        Itinerary privateButShared = itinerary(2L, otherOwner, Visibility.PRIVATE);

        when(itineraryRepository.findByOwnerId(userId)).thenReturn(List.of());
        when(itineraryShareRepository.findBySharedWithUserId(userId))
                .thenReturn(List.of(share(2L, userId)));
        when(itineraryRepository.findAllById(List.of(2L)))
                .thenReturn(List.of(privateButShared));

        List<ItineraryResponse> result = itineraryService.getAllAccessibleItineraries(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void shareItinerary_utenteEsistente_impostaVisibilitaShared() {
        Itinerary itinerary = itinerary(1L, userId, Visibility.PRIVATE);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(userAuthClient.userExists(sharedUserId)).thenReturn(true);
        when(itineraryShareRepository.existsByItineraryIdAndSharedWithUserId(1L, sharedUserId)).thenReturn(false);
        when(itineraryShareRepository.save(any(ItineraryShare.class))).thenAnswer(inv -> inv.getArgument(0));

        ItineraryShareResponse response = itineraryService.shareItinerary(
                1L, new ShareItineraryRequest(sharedUserId, true), userId);

        assertThat(response.itineraryId()).isEqualTo(1L);
        assertThat(response.sharedWithUserId()).isEqualTo(sharedUserId);
        assertThat(response.canEdit()).isTrue();
        assertThat(itinerary.getVisibility()).isEqualTo(Visibility.SHARED);
        verify(itineraryRepository).save(itinerary);
    }

    @Test
    void getShares_mappaEntityInResponse() {
        Itinerary itinerary = itinerary(1L, userId, Visibility.SHARED);
        ItineraryShare share = share(1L, sharedUserId);
        share.setCanEdit(true);

        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(itineraryShareRepository.findByItineraryId(1L)).thenReturn(List.of(share));

        List<ItineraryShareResponse> result = itineraryService.getShares(1L, userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).itineraryId()).isEqualTo(1L);
        assertThat(result.get(0).sharedWithUserId()).isEqualTo(sharedUserId);
        assertThat(result.get(0).canEdit()).isTrue();
    }

    @Test
    void shareItinerary_utenteInesistente_lanciaResourceNotFound() {
        Itinerary itinerary = itinerary(1L, userId, Visibility.PRIVATE);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(userAuthClient.userExists(sharedUserId)).thenReturn(false);

        assertThatThrownBy(() -> itineraryService.shareItinerary(
                1L, new ShareItineraryRequest(sharedUserId, true), userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(sharedUserId.toString());

        verify(itineraryShareRepository, never()).save(any(ItineraryShare.class));
        verify(itineraryRepository, never()).save(any(Itinerary.class));
    }

    @Test
    void shareItinerary_giaCondiviso_lanciaIllegalArgument() {
        Itinerary itinerary = itinerary(1L, userId, Visibility.SHARED);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(userAuthClient.userExists(sharedUserId)).thenReturn(true);
        when(itineraryShareRepository.existsByItineraryIdAndSharedWithUserId(1L, sharedUserId)).thenReturn(true);

        assertThatThrownBy(() -> itineraryService.shareItinerary(
                1L, new ShareItineraryRequest(sharedUserId, false), userId))
                .isInstanceOf(IllegalArgumentException.class);

        verify(itineraryRepository, never()).save(any(Itinerary.class));
    }

    @Test
    void shareItinerary_suPubblico_nonDegradaLaVisibilita() {
        Itinerary itinerary = itinerary(1L, userId, Visibility.PUBLIC);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(userAuthClient.userExists(sharedUserId)).thenReturn(true);
        when(itineraryShareRepository.existsByItineraryIdAndSharedWithUserId(1L, sharedUserId))
                .thenReturn(false);
        when(itineraryShareRepository.save(any(ItineraryShare.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        itineraryService.shareItinerary(1L, new ShareItineraryRequest(sharedUserId, true), userId);

        assertThat(itinerary.getVisibility()).isEqualTo(Visibility.PUBLIC);
        verify(itineraryRepository, never()).save(any(Itinerary.class));
    }

    @Test
    void shareItinerary_giaShared_nonSalvaDiNuovoLaVisibilita() {
        Itinerary itinerary = itinerary(1L, userId, Visibility.SHARED);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(userAuthClient.userExists(sharedUserId)).thenReturn(true);
        when(itineraryShareRepository.existsByItineraryIdAndSharedWithUserId(1L, sharedUserId)).thenReturn(false);
        when(itineraryShareRepository.save(any(ItineraryShare.class))).thenAnswer(inv -> inv.getArgument(0));

        itineraryService.shareItinerary(1L, new ShareItineraryRequest(sharedUserId, true), userId);

        assertThat(itinerary.getVisibility()).isEqualTo(Visibility.SHARED);
        verify(itineraryRepository, never()).save(any(Itinerary.class));
    }
}
