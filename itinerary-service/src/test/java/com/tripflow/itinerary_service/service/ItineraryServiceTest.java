package com.tripflow.itinerary_service.service;

import com.tripflow.itinerary_service.client.UserAuthClient;
import com.tripflow.itinerary_service.dto.request.AddItineraryStopRequest;
import com.tripflow.itinerary_service.dto.request.CreateItineraryRequest;
import com.tripflow.itinerary_service.dto.request.UpdateItineraryStopRequest;
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
import com.tripflow.itinerary_service.model.ItineraryStop;
import com.tripflow.itinerary_service.model.Visibility;
import com.tripflow.itinerary_service.repository.ItineraryRepository;
import com.tripflow.itinerary_service.repository.ItineraryShareRepository;
import com.tripflow.itinerary_service.repository.ItineraryStopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
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

    private final TransactionTemplate transactionTemplate = new TransactionTemplate(new PlatformTransactionManager() {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    });

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
                userAuthClient,
                transactionTemplate
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
        assertThat(response.publicToken()).isNotNull();
    }

    @Test
    void createItinerary_visibilityPublic_generaPublicToken() {
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateItineraryRequest request = new CreateItineraryRequest(
                "Pubblico esplicito", "descrizione", Visibility.PUBLIC, null, null, null);

        ItineraryResponse response = itineraryService.createItinerary(request, userId);

        assertThat(response.visibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(response.publicToken()).isNotNull();
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
        verify(itineraryShareRepository, never()).deleteByItineraryId(1L);
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
    void updateVisibility_senzaVisibilita_lanciaIllegalArgument() {
        assertThatThrownBy(() -> itineraryService.updateVisibility(
                1L, new UpdateVisibilityRequest(null, null), userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Almeno uno tra visibility e isPublic deve essere specificato");

        verify(itineraryRepository, never()).findById(any());
        verify(itineraryRepository, never()).save(any(Itinerary.class));
        verify(itineraryShareRepository, never()).deleteByItineraryId(any());
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
        verify(itineraryShareRepository).deleteByItineraryId(1L);
    }

    @Test
    void updateVisibility_daSharedAPrivate_eliminaLeShare() {
        Itinerary itinerary = itinerary(1L, userId, Visibility.SHARED);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(itineraryRepository.save(itinerary)).thenReturn(itinerary);

        ItineraryResponse response = itineraryService.updateVisibility(
                1L, new UpdateVisibilityRequest(Visibility.PRIVATE, null), userId);

        assertThat(response.visibility()).isEqualTo(Visibility.PRIVATE);
        verify(itineraryShareRepository).deleteByItineraryId(1L);
    }

    @Test
    void getPublicItinerary_tokenValido_restituisceItinerario() {
        UUID token = UUID.fromString("12345678-1234-1234-1234-123456789012");
        Itinerary itinerary = itinerary(1L, otherOwner, Visibility.PUBLIC);
        itinerary.setPublicToken(token);

        when(itineraryRepository.findByPublicTokenAndVisibility(token, Visibility.PUBLIC))
                .thenReturn(Optional.of(itinerary));

        var response = itineraryService.getPublicItinerary(token);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Itinerario 1");
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
    void shareItinerary_verificaUtentePrimaDelSalvataggio() {
        Itinerary itinerary = itinerary(1L, userId, Visibility.PRIVATE);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(userAuthClient.userExists(sharedUserId)).thenReturn(true);
        when(itineraryShareRepository.existsByItineraryIdAndSharedWithUserId(1L, sharedUserId)).thenReturn(false);
        when(itineraryShareRepository.save(any(ItineraryShare.class))).thenAnswer(inv -> inv.getArgument(0));

        itineraryService.shareItinerary(1L, new ShareItineraryRequest(sharedUserId, true), userId);

        InOrder inOrder = inOrder(itineraryRepository, userAuthClient, itineraryShareRepository);
        inOrder.verify(itineraryRepository).findById(1L);
        inOrder.verify(userAuthClient).userExists(sharedUserId);
        inOrder.verify(itineraryShareRepository).save(any(ItineraryShare.class));
    }

    @Test
    void shareItinerary_nonEAnnotatoTransactional() throws NoSuchMethodException {
        Method method = ItineraryService.class.getMethod(
                "shareItinerary", Long.class, ShareItineraryRequest.class, UUID.class);

        assertThat(method.getAnnotation(Transactional.class)).isNull();
        assertThat(ItineraryService.class.getAnnotation(Transactional.class)).isNull();
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
    @Test
    void deleteStop_rimuoveLaTappaDallaCollectionEDalRepository() {
        Itinerary itinerary = itinerary(1L, userId, Visibility.PRIVATE);
        ItineraryStop stop = new ItineraryStop();
        stop.setId(10L);
        stop.setItinerary(itinerary);
        itinerary.getStops().add(stop);

        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));

        itineraryService.deleteStop(1L, 10L, userId);

        assertThat(itinerary.getStops()).doesNotContain(stop);
        verify(itineraryStopRepository).delete(stop);
        // la tappa va cercata nella collection gestita, non con una query separata
        verify(itineraryStopRepository, never()).findById(any());
    }

    @Test
    void deleteStop_tappaNonAppartenenteAllItinerario_lanciaResourceNotFound() {
        Itinerary itinerary = itinerary(1L, userId, Visibility.PRIVATE);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));

        assertThatThrownBy(() -> itineraryService.deleteStop(1L, 99L, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(itineraryStopRepository, never()).delete(any());
    }

    @Test
    void deleteStop_editorSenzaPermessi_lanciaAccessDenied() {
        Itinerary itinerary = itinerary(1L, otherOwner, Visibility.SHARED);
        ItineraryStop stop = new ItineraryStop();
        stop.setId(10L);
        stop.setItinerary(itinerary);
        itinerary.getStops().add(stop);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(itineraryShareRepository.findByItineraryIdAndSharedWithUserId(1L, userId))
                .thenReturn(Optional.of(share(1L, userId)));

        assertThatThrownBy(() -> itineraryService.deleteStop(1L, 10L, userId))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(itinerary.getStops()).contains(stop);
        verify(itineraryStopRepository, never()).delete(any());
    }

    @Test
    void createItinerary_privato_nonGeneraPublicToken() {
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        ItineraryResponse response = itineraryService.createItinerary(
                new CreateItineraryRequest("Privato", null, Visibility.PRIVATE, null, null, null), userId);

        assertThat(response.publicToken()).isNull();
        assertThat(response.canEdit()).isTrue();
    }

    @Test
    void createItinerary_isPublicTrue_generaPublicTokenGiaSalvato() {
        when(itineraryRepository.save(any(Itinerary.class))).thenAnswer(inv -> {
            Itinerary toSave = inv.getArgument(0);
            // il token deve essere gia' valorizzato al momento del salvataggio
            assertThat(toSave.getPublicToken()).isNotNull();
            return toSave;
        });

        ItineraryResponse response = itineraryService.createItinerary(
                new CreateItineraryRequest("Pubblico", null, null, true, null, null), userId);

        assertThat(response.visibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(response.publicToken()).isNotNull();
    }

    @Test
    void getAllAccessibleItineraries_canEditCalcolatoDalleShareSenzaQueryAggiuntive() {
        Itinerary owned = itinerary(1L, userId, Visibility.PRIVATE);
        Itinerary sharedReadOnly = itinerary(2L, otherOwner, Visibility.SHARED);
        Itinerary sharedEditable = itinerary(3L, otherOwner, Visibility.PUBLIC);
        ItineraryShare readOnly = share(2L, userId);
        ItineraryShare editable = share(3L, userId);
        editable.setCanEdit(true);

        when(itineraryRepository.findByOwnerId(userId)).thenReturn(List.of(owned));
        when(itineraryShareRepository.findBySharedWithUserId(userId)).thenReturn(List.of(readOnly, editable));
        when(itineraryRepository.findAllById(List.of(2L, 3L))).thenReturn(List.of(sharedReadOnly, sharedEditable));

        List<ItineraryResponse> result = itineraryService.getAllAccessibleItineraries(userId);

        assertThat(result).extracting(ItineraryResponse::id).containsExactly(1L, 2L, 3L);
        assertThat(result).extracting(ItineraryResponse::canEdit).containsExactly(true, false, true);
        verify(itineraryShareRepository, never()).findByItineraryIdAndSharedWithUserId(any(), any());
    }

    @Test
    void getItinerariesByOwner_canEditSempreTrue() {
        when(itineraryRepository.findByOwnerId(userId)).thenReturn(List.of(itinerary(1L, userId, Visibility.PUBLIC)));

        assertThat(itineraryService.getItinerariesByOwner(userId))
                .singleElement()
                .extracting(ItineraryResponse::canEdit)
                .isEqualTo(true);
    }

    @Test
    void getPublicItinerary_nonEsponeDatiDellOwner() {
        // La vista pubblica non ha proprio i campi ownerId/canEdit/publicToken.
        assertThat(java.util.Arrays.stream(
                com.tripflow.itinerary_service.dto.response.PublicItineraryResponse.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName))
                .doesNotContain("ownerId", "canEdit", "publicToken", "visibility");
    }

    @Test
    void removeShare_ultimaCondivisione_itinerarioSharedTornaPrivate() {
        Itinerary shared = itinerary(1L, userId, Visibility.SHARED);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(shared));
        when(itineraryShareRepository.existsByItineraryIdAndSharedWithUserId(1L, sharedUserId)).thenReturn(true);
        when(itineraryShareRepository.existsByItineraryId(1L)).thenReturn(false);

        itineraryService.removeShare(1L, sharedUserId, userId);

        verify(itineraryShareRepository).deleteByItineraryIdAndSharedWithUserId(1L, sharedUserId);
        assertThat(shared.getVisibility()).isEqualTo(Visibility.PRIVATE);
        verify(itineraryRepository).save(shared);
    }

    @Test
    void removeShare_restanoAltreCondivisioni_restaShared() {
        Itinerary shared = itinerary(1L, userId, Visibility.SHARED);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(shared));
        when(itineraryShareRepository.existsByItineraryIdAndSharedWithUserId(1L, sharedUserId)).thenReturn(true);
        when(itineraryShareRepository.existsByItineraryId(1L)).thenReturn(true);

        itineraryService.removeShare(1L, sharedUserId, userId);

        assertThat(shared.getVisibility()).isEqualTo(Visibility.SHARED);
        verify(itineraryRepository, never()).save(any());
    }

    @Test
    void removeShare_itinerarioPublic_restaPublic() {
        Itinerary pub = itinerary(1L, userId, Visibility.PUBLIC);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(pub));
        when(itineraryShareRepository.existsByItineraryIdAndSharedWithUserId(1L, sharedUserId)).thenReturn(true);

        itineraryService.removeShare(1L, sharedUserId, userId);

        assertThat(pub.getVisibility()).isEqualTo(Visibility.PUBLIC);
        verify(itineraryShareRepository, never()).existsByItineraryId(any());
        verify(itineraryRepository, never()).save(any());
    }

    @Test
    void getAllAccessibleItineraries_includeCondivisoDivenutoPublic() {
        Itinerary nowPublic = itinerary(2L, otherOwner, Visibility.PUBLIC);

        when(itineraryRepository.findByOwnerId(userId)).thenReturn(List.of());
        when(itineraryShareRepository.findBySharedWithUserId(userId)).thenReturn(List.of(share(2L, userId)));
        when(itineraryRepository.findAllById(List.of(2L))).thenReturn(List.of(nowPublic));

        assertThat(itineraryService.getAllAccessibleItineraries(userId)).hasSize(1);
    }

    @Test
    void getItineraryById_canEdit_trueSoloPerOwnerOEditorCondiviso() {
        Itinerary own = itinerary(1L, userId, Visibility.PRIVATE);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(own));
        assertThat(itineraryService.getItineraryById(1L, userId).canEdit()).isTrue();

        Itinerary shared = itinerary(2L, otherOwner, Visibility.SHARED);
        ItineraryShare readOnly = share(2L, userId);
        when(itineraryRepository.findById(2L)).thenReturn(Optional.of(shared));
        when(itineraryShareRepository.findByItineraryIdAndSharedWithUserId(2L, userId))
                .thenReturn(Optional.of(readOnly));
        assertThat(itineraryService.getItineraryById(2L, userId).canEdit()).isFalse();

        readOnly.setCanEdit(true);
        assertThat(itineraryService.getItineraryById(2L, userId).canEdit()).isTrue();
    }

    // ---------------------------------------------------------------- Parte 2

    private Itinerary datedItinerary() {
        Itinerary itinerary = itinerary(1L, userId, Visibility.PRIVATE);
        itinerary.setStartDate(LocalDate.of(2030, 5, 1));
        itinerary.setEndDate(LocalDate.of(2030, 5, 3));
        return itinerary;
    }

    private AddItineraryStopRequest stopRequest(int order, String type, UUID viaggioId, UUID attivitaId,
                                                LocalDateTime start, LocalDateTime end) {
        return new AddItineraryStopRequest(order, type, viaggioId, attivitaId, "Tappa", null, start, end, null);
    }

    private final LocalDateTime inRangeStart = LocalDateTime.of(2030, 5, 1, 10, 0);
    private final LocalDateTime inRangeEnd = LocalDateTime.of(2030, 5, 1, 12, 0);

    @Test
    void addStop_stopTypeMinuscolo_lanciaIllegalArgumentConMessaggio() {
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(datedItinerary()));

        assertThatThrownBy(() -> itineraryService.addStopToItinerary(1L,
                stopRequest(1, "custom", null, null, inRangeStart, inRangeEnd), userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stopType non valido: custom");
        verify(itineraryStopRepository, never()).save(any());
    }

    @Test
    void addStop_tripSenzaViaggioId_lanciaIllegalArgument() {
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(datedItinerary()));

        assertThatThrownBy(() -> itineraryService.addStopToItinerary(1L,
                stopRequest(1, "TRIP", null, null, inRangeStart, inRangeEnd), userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Una tappa di tipo TRIP richiede viaggioId");
    }

    @Test
    void addStop_activityConViaggioId_lanciaIllegalArgument() {
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(datedItinerary()));

        assertThatThrownBy(() -> itineraryService.addStopToItinerary(1L,
                stopRequest(1, "ACTIVITY", UUID.randomUUID(), UUID.randomUUID(), inRangeStart, inRangeEnd), userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Una tappa di tipo ACTIVITY non puo avere viaggioId");
    }

    @Test
    void addStop_fuoriDalRangeDellItinerario_lanciaIllegalArgument() {
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(datedItinerary()));

        assertThatThrownBy(() -> itineraryService.addStopToItinerary(1L,
                stopRequest(1, "CUSTOM", null, null,
                        LocalDateTime.of(2030, 5, 4, 10, 0), LocalDateTime.of(2030, 5, 4, 12, 0)), userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("successive alla fine dell'itinerario");
    }

    private ItineraryStop existingStop(long id, int order) {
        ItineraryStop existing = new ItineraryStop();
        existing.setId(id);
        existing.setStopOrder(order);
        return existing;
    }

    @Test
    void addStop_stopOrderGiaUsato_vieneAccodataInFondo() {
        Itinerary itinerary = datedItinerary();
        itinerary.getStops().add(existingStop(10L, 1));
        itinerary.getStops().add(existingStop(11L, 4));
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(itineraryStopRepository.save(any(ItineraryStop.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = itineraryService.addStopToItinerary(1L,
                stopRequest(1, "CUSTOM", null, null, inRangeStart, inRangeEnd), userId);

        // posizione 1 occupata -> ultima (4) + 1; la response riporta lo stopOrder effettivo
        assertThat(response.stopOrder()).isEqualTo(5);
    }

    @Test
    void addStop_stopOrderLibero_vieneRispettato() {
        Itinerary itinerary = datedItinerary();
        itinerary.getStops().add(existingStop(10L, 1));
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(itineraryStopRepository.save(any(ItineraryStop.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = itineraryService.addStopToItinerary(1L,
                stopRequest(3, "CUSTOM", null, null, inRangeStart, inRangeEnd), userId);

        assertThat(response.stopOrder()).isEqualTo(3);
    }

    @Test
    void updateStop_stopOrderGiaUsato_restaConflitto409() {
        Itinerary itinerary = datedItinerary();
        ItineraryStop other = existingStop(10L, 1);
        ItineraryStop stop = existingStop(11L, 2);
        stop.setStopType("CUSTOM");
        stop.setItinerary(itinerary);
        itinerary.getStops().add(other);
        itinerary.getStops().add(stop);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(itineraryStopRepository.findById(11L)).thenReturn(Optional.of(stop));

        assertThatThrownBy(() -> itineraryService.updateStop(1L, 11L,
                new UpdateItineraryStopRequest(1, null, null, null, null, null, null, null, null), userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stopOrder 1");
    }

    @Test
    void addStop_valida_vieneAggiuntaAllaCollectionESalvata() {
        Itinerary itinerary = datedItinerary();
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(itineraryStopRepository.save(any(ItineraryStop.class))).thenAnswer(inv -> inv.getArgument(0));

        itineraryService.addStopToItinerary(1L, stopRequest(1, "CUSTOM", null, null, inRangeStart, inRangeEnd), userId);

        assertThat(itinerary.getStops()).hasSize(1);
        verify(itineraryStopRepository).save(any(ItineraryStop.class));
    }

    @Test
    void updateStop_cambioTipoDaTripACustom_azzeraIRiferimenti() {
        Itinerary itinerary = datedItinerary();
        ItineraryStop stop = new ItineraryStop();
        stop.setId(10L);
        stop.setStopOrder(1);
        stop.setStopType("TRIP");
        stop.setViaggioId(UUID.randomUUID());
        stop.setItinerary(itinerary);
        stop.setStartDatetime(inRangeStart);
        stop.setEndDatetime(inRangeEnd);
        itinerary.getStops().add(stop);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(itineraryStopRepository.findById(10L)).thenReturn(Optional.of(stop));
        when(itineraryStopRepository.save(any(ItineraryStop.class))).thenAnswer(inv -> inv.getArgument(0));

        itineraryService.updateStop(1L, 10L,
                new UpdateItineraryStopRequest(null, "CUSTOM", null, null, null, null, null, null, null), userId);

        assertThat(stop.getStopType()).isEqualTo("CUSTOM");
        assertThat(stop.getViaggioId()).isNull();
    }

    @Test
    void updateStop_nuovaFinePrimaDellInizioSalvato_lanciaIllegalArgument() {
        Itinerary itinerary = datedItinerary();
        ItineraryStop stop = new ItineraryStop();
        stop.setId(10L);
        stop.setStopOrder(1);
        stop.setStopType("CUSTOM");
        stop.setItinerary(itinerary);
        stop.setStartDatetime(inRangeStart);
        stop.setEndDatetime(inRangeEnd);
        itinerary.getStops().add(stop);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));
        when(itineraryStopRepository.findById(10L)).thenReturn(Optional.of(stop));

        assertThatThrownBy(() -> itineraryService.updateStop(1L, 10L,
                new UpdateItineraryStopRequest(null, null, null, null, null, null, null,
                        LocalDateTime.of(2030, 5, 1, 8, 0), null), userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("precedente alla data di inizio");
    }

    @Test
    void updateItinerary_nuoveDateEscludonoUnaTappa_lanciaIllegalArgument() {
        Itinerary itinerary = datedItinerary();
        ItineraryStop stop = new ItineraryStop();
        stop.setId(10L);
        stop.setStopOrder(1);
        stop.setStartDatetime(LocalDateTime.of(2030, 5, 3, 10, 0));
        stop.setEndDatetime(LocalDateTime.of(2030, 5, 3, 12, 0));
        itinerary.getStops().add(stop);
        when(itineraryRepository.findById(1L)).thenReturn(Optional.of(itinerary));

        assertThatThrownBy(() -> itineraryService.updateItinerary(1L,
                new UpdateItineraryRequest("Accorciato", null, LocalDate.of(2030, 5, 1), LocalDate.of(2030, 5, 2)), userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tappa 10");
        verify(itineraryRepository, never()).save(any());
    }

    @Test
    void shareItinerary_conSeStesso_lanciaIllegalArgumentSenzaChiamateRemote() {
        assertThatThrownBy(() -> itineraryService.shareItinerary(1L, new ShareItineraryRequest(userId, true), userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Non puoi condividere un itinerario con te stesso");
        verify(userAuthClient, never()).userExists(any());
        verify(itineraryShareRepository, never()).save(any());
    }
}
