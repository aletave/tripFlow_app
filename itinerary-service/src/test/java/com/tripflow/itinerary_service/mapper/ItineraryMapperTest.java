package com.tripflow.itinerary_service.mapper;

import com.tripflow.itinerary_service.dto.request.AddItineraryStopRequest;
import com.tripflow.itinerary_service.dto.request.CreateItineraryRequest;
import com.tripflow.itinerary_service.dto.request.UpdateItineraryStopRequest;
import com.tripflow.itinerary_service.dto.response.ItineraryResponse;
import com.tripflow.itinerary_service.dto.response.ItineraryStopResponse;
import com.tripflow.itinerary_service.model.Itinerary;
import com.tripflow.itinerary_service.model.ItineraryStop;
import com.tripflow.itinerary_service.model.Visibility;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ItineraryMapperTest {

    private static final UUID OWNER_ID = UUID.fromString("70cd15d7-3a55-407c-9909-1f575e86cbfe");

    private final ItineraryMapper mapper = new ItineraryMapper();

    @Test
    void toEntity_usaIsPublicQuandoVisibilityAssente() {
        CreateItineraryRequest request = new CreateItineraryRequest(
                "Vacanza", "descrizione", null, true,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3));

        Itinerary entity = mapper.toEntity(request, OWNER_ID);

        assertThat(entity.getVisibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(entity.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(entity.getTitle()).isEqualTo("Vacanza");
        assertThat(entity.getStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void toEntity_enumHaPrioritaSulBooleano() {
        CreateItineraryRequest request = new CreateItineraryRequest(
                "Vacanza", null, Visibility.SHARED, true, null, null);

        Itinerary entity = mapper.toEntity(request, OWNER_ID);

        assertThat(entity.getVisibility()).isEqualTo(Visibility.SHARED);
    }

    @Test
    void toEntity_senzaVisibilita_defaultPrivate() {
        CreateItineraryRequest request = new CreateItineraryRequest(
                "Vacanza", null, null, null, null, null);

        Itinerary entity = mapper.toEntity(request, OWNER_ID);

        assertThat(entity.getVisibility()).isEqualTo(Visibility.PRIVATE);
    }

    @Test
    void toResponse_mappaItinerarioETappe() {
        Itinerary itinerary = new Itinerary();
        itinerary.setId(1L);
        itinerary.setOwnerId(OWNER_ID);
        itinerary.setTitle("Vacanza");
        itinerary.setDescription("descrizione");
        itinerary.setVisibility(Visibility.PUBLIC);
        itinerary.setStartDate(LocalDate.of(2026, 9, 1));
        itinerary.setEndDate(LocalDate.of(2026, 9, 3));
        itinerary.setCreatedAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        itinerary.setUpdatedAt(LocalDateTime.of(2026, 8, 2, 11, 0));

        ItineraryStop stop = new ItineraryStop();
        stop.setId(10L);
        stop.setStopOrder(1);
        stop.setStopType("ACTIVITY");
        stop.setCustomTitle("Colosseo");
        stop.setStartDatetime(LocalDateTime.of(2026, 9, 1, 10, 0));
        stop.setEndDatetime(LocalDateTime.of(2026, 9, 1, 12, 0));
        stop.setItinerary(itinerary);
        itinerary.getStops().add(stop);

        ItineraryResponse response = mapper.toResponse(itinerary);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.ownerId()).isEqualTo(OWNER_ID);
        assertThat(response.visibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(response.stops()).hasSize(1);
        assertThat(response.stops().get(0).customTitle()).isEqualTo("Colosseo");
        assertThat(response.stops().get(0).stopOrder()).isEqualTo(1);
    }

    @Test
    void toStopEntity_mappaTuttiICampi() {
        Itinerary itinerary = new Itinerary();
        itinerary.setId(1L);

        AddItineraryStopRequest request = new AddItineraryStopRequest(
                2, "ACTIVITY", null, UUID.randomUUID(),
                "Colosseo", "Tour", LocalDateTime.of(2026, 9, 1, 10, 0),
                LocalDateTime.of(2026, 9, 1, 12, 0), "note");

        ItineraryStop stop = mapper.toStopEntity(request, itinerary);

        assertThat(stop.getStopOrder()).isEqualTo(2);
        assertThat(stop.getStopType()).isEqualTo("ACTIVITY");
        assertThat(stop.getCustomTitle()).isEqualTo("Colosseo");
        assertThat(stop.getItinerary()).isSameAs(itinerary);
    }

    @Test
    void updateEntity_aggiornaCampiERisolveVisibilita() {
        Itinerary itinerary = new Itinerary();
        itinerary.setVisibility(Visibility.PRIVATE);

        CreateItineraryRequest request = new CreateItineraryRequest(
                "Nuovo titolo", "nuova descrizione", null, true,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5));

        mapper.updateEntity(request, itinerary);

        assertThat(itinerary.getTitle()).isEqualTo("Nuovo titolo");
        assertThat(itinerary.getDescription()).isEqualTo("nuova descrizione");
        assertThat(itinerary.getVisibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(itinerary.getStartDate()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void updateStopEntity_ignoraCampiNull() {
        ItineraryStop stop = new ItineraryStop();
        stop.setStopOrder(1);
        stop.setCustomTitle("Originale");
        stop.setNotes("nota originale");

        UpdateItineraryStopRequest request = new UpdateItineraryStopRequest(
                null, null, null, null, "Aggiornato", null, null, null, null);

        mapper.updateStopEntity(request, stop);

        assertThat(stop.getStopOrder()).isEqualTo(1);
        assertThat(stop.getCustomTitle()).isEqualTo("Aggiornato");
        assertThat(stop.getNotes()).isEqualTo("nota originale");
    }

    @Test
    void toStopResponse_mappaTuttiICampi() {
        ItineraryStop stop = new ItineraryStop();
        stop.setId(10L);
        stop.setStopOrder(3);
        stop.setStopType("CUSTOM");
        stop.setCustomTitle("Passeggiata");
        stop.setCustomDescription("Centro storico");
        stop.setViaggioId(null);
        stop.setAttivitaId(null);
        stop.setStartDatetime(LocalDateTime.of(2026, 9, 1, 15, 0));
        stop.setEndDatetime(LocalDateTime.of(2026, 9, 1, 17, 0));
        stop.setNotes("note");

        ItineraryStopResponse response = mapper.toStopResponse(stop);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.stopOrder()).isEqualTo(3);
        assertThat(response.stopType()).isEqualTo("CUSTOM");
        assertThat(response.customTitle()).isEqualTo("Passeggiata");
        assertThat(response.notes()).isEqualTo("note");
    }
}
