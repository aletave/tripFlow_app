package com.tripflow.itinerary_service.repository;

import com.tripflow.itinerary_service.client.UserAuthClient;
import com.tripflow.itinerary_service.dto.request.CreateItineraryRequest;
import com.tripflow.itinerary_service.dto.response.ItineraryResponse;
import com.tripflow.itinerary_service.mapper.ItineraryMapper;
import com.tripflow.itinerary_service.mapper.ItineraryShareMapper;
import com.tripflow.itinerary_service.model.Itinerary;
import com.tripflow.itinerary_service.model.ItineraryStop;
import com.tripflow.itinerary_service.model.Visibility;
import com.tripflow.itinerary_service.service.ItineraryService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Test di integrazione JPA reale (H2 in memoria): verifica che la DELETE della tappa
// arrivi davvero al database nonostante @EntityGraph("stops") + cascade ALL.
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:itinerarytest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@Import({ItineraryService.class, ItineraryMapper.class, ItineraryShareMapper.class})
class ItineraryStopDeletionJpaTest {

    @Autowired
    private ItineraryService itineraryService;
    @Autowired
    private ItineraryRepository itineraryRepository;
    @Autowired
    private ItineraryStopRepository itineraryStopRepository;
    @Autowired
    private EntityManager entityManager;

    @MockBean
    private UserAuthClient userAuthClient;

    private final UUID ownerId = UUID.fromString("70cd15d7-3a55-407c-9909-1f575e86cbfe");

    private Long[] persistItineraryWithTwoStops() {
        Itinerary itinerary = new Itinerary();
        itinerary.setOwnerId(ownerId);
        itinerary.setTitle("Weekend a Roma");
        itinerary.setVisibility(Visibility.PRIVATE);
        itinerary.setStartDate(LocalDate.of(2030, 5, 1));
        itinerary.setEndDate(LocalDate.of(2030, 5, 3));

        ItineraryStop first = stop(itinerary, 1, "Colosseo");
        ItineraryStop second = stop(itinerary, 2, "Fori Imperiali");
        itinerary.getStops().add(first);
        itinerary.getStops().add(second);

        itineraryRepository.saveAndFlush(itinerary);
        Long[] ids = {itinerary.getId(), first.getId(), second.getId()};
        entityManager.clear();
        return ids;
    }

    private ItineraryStop stop(Itinerary itinerary, int order, String title) {
        ItineraryStop stop = new ItineraryStop();
        stop.setItinerary(itinerary);
        stop.setStopOrder(order);
        stop.setStopType("CUSTOM");
        stop.setCustomTitle(title);
        stop.setStartDatetime(LocalDateTime.of(2030, 5, 1, 9 + order, 0));
        stop.setEndDatetime(LocalDateTime.of(2030, 5, 1, 10 + order, 0));
        return stop;
    }

    @Test
    void deleteStop_rimuoveDavveroLaRigaDalDatabase() {
        Long[] ids = persistItineraryWithTwoStops();

        itineraryService.deleteStop(ids[0], ids[1], ownerId);
        entityManager.flush();
        entityManager.clear();

        assertThat(itineraryStopRepository.findById(ids[1])).isEmpty();
        assertThat(itineraryStopRepository.findById(ids[2])).isPresent();
        assertThat(itineraryRepository.findById(ids[0]).orElseThrow().getStops())
                .extracting(ItineraryStop::getId)
                .containsExactly(ids[2]);
    }

    // Documenta la regressione: con la tappa ancora nella collection caricata via
    // @EntityGraph, la sola delete dal repository viene annullata dalla cascade.
    @Test
    void soloRepositoryDelete_conTappaNellaCollection_nonCancellaLaRiga() {
        Long[] ids = persistItineraryWithTwoStops();

        Itinerary itinerary = itineraryRepository.findById(ids[0]).orElseThrow();
        ItineraryStop stop = itinerary.getStops().stream()
                .filter(s -> s.getId().equals(ids[1]))
                .findFirst()
                .orElseThrow();
        itineraryStopRepository.delete(stop);
        entityManager.flush();
        entityManager.clear();

        assertThat(itineraryStopRepository.findById(ids[1])).isPresent();
    }

    @Test
    void createItinerary_public_persisteIlPublicTokenNelDatabase() {
        ItineraryResponse response = itineraryService.createItinerary(
                new CreateItineraryRequest("Pubblico", null, Visibility.PUBLIC, null, null, null), ownerId);
        entityManager.flush();
        entityManager.clear();

        Itinerary saved = itineraryRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getPublicToken()).isNotNull().isEqualTo(response.publicToken());
        assertThat(itineraryRepository.findByPublicTokenAndVisibility(saved.getPublicToken(), Visibility.PUBLIC))
                .isPresent();
    }

    @Test
    void deleteBulk_rimuoveTappeTripEActivityDelViaggioEliminato() {
        UUID viaggioId = UUID.randomUUID();
        UUID attivitaId = UUID.randomUUID();
        Itinerary itinerary = new Itinerary();
        itinerary.setOwnerId(ownerId);
        itinerary.setTitle("Con riferimenti al catalogo");
        itinerary.setVisibility(Visibility.PRIVATE);
        ItineraryStop trip = stop(itinerary, 1, null);
        trip.setStopType("TRIP");
        trip.setViaggioId(viaggioId);
        ItineraryStop activity = stop(itinerary, 2, null);
        activity.setStopType("ACTIVITY");
        activity.setAttivitaId(attivitaId);
        ItineraryStop custom = stop(itinerary, 3, "Resta");
        itinerary.getStops().addAll(java.util.List.of(trip, activity, custom));
        itineraryRepository.saveAndFlush(itinerary);
        entityManager.clear();

        assertThat(itineraryStopRepository.deleteByViaggioId(viaggioId)).isEqualTo(1);
        assertThat(itineraryStopRepository.deleteByAttivitaIdIn(java.util.Set.of(attivitaId))).isEqualTo(1);
        entityManager.clear();

        assertThat(itineraryStopRepository.findByItineraryIdOrderByStopOrderAsc(itinerary.getId()))
                .extracting(ItineraryStop::getId)
                .containsExactly(custom.getId());
    }

    @Autowired
    private ItineraryShareRepository itineraryShareRepository;

    @Test
    void removeShare_ultimaCondivisione_sulDatabaseTornaPrivate() {
        Itinerary itinerary = new Itinerary();
        itinerary.setOwnerId(ownerId);
        itinerary.setTitle("Condiviso");
        itinerary.setVisibility(Visibility.SHARED);
        itineraryRepository.saveAndFlush(itinerary);
        UUID friend = UUID.randomUUID();
        com.tripflow.itinerary_service.model.ItineraryShare share = new com.tripflow.itinerary_service.model.ItineraryShare();
        share.setItineraryId(itinerary.getId());
        share.setSharedWithUserId(friend);
        itineraryShareRepository.saveAndFlush(share);
        entityManager.clear();

        itineraryService.removeShare(itinerary.getId(), friend, ownerId);
        entityManager.flush();
        entityManager.clear();

        assertThat(itineraryShareRepository.findByItineraryId(itinerary.getId())).isEmpty();
        assertThat(itineraryRepository.findById(itinerary.getId()).orElseThrow().getVisibility())
                .isEqualTo(Visibility.PRIVATE);
    }
}
