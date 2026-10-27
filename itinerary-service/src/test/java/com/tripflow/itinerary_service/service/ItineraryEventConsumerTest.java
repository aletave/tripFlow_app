package com.tripflow.itinerary_service.service;

import com.tripflow.itinerary_service.dto.EventDTO;
import com.tripflow.itinerary_service.repository.ItineraryStopRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItineraryEventConsumerTest {

    @Mock
    private ItineraryStopRepository itineraryStopRepository;

    @InjectMocks
    private ItineraryEventConsumer consumer;

    private final UUID tripId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private final UUID activity1 = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private final UUID activity2 = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    @Test
    void tripDeleted_eliminaTappeTripEActivityOrfane() {
        Map<String, Object> payload = Map.of(
                "id", tripId.toString(),
                "activities", List.of(Map.of("id", activity1.toString()), Map.of("id", activity2.toString())));
        when(itineraryStopRepository.deleteByViaggioId(tripId)).thenReturn(1);
        when(itineraryStopRepository.deleteByAttivitaIdIn(Set.of(activity1, activity2))).thenReturn(2);

        consumer.handleTripDeletedEvent(new EventDTO<>("TRIP_DELETED", payload));

        verify(itineraryStopRepository).deleteByViaggioId(tripId);
        verify(itineraryStopRepository).deleteByAttivitaIdIn(Set.of(activity1, activity2));
    }

    @Test
    void tripDeletedSenzaAttivita_eliminaSoloTappeTrip() {
        consumer.handleTripDeletedEvent(new EventDTO<>("TRIP_DELETED", Map.of("id", tripId.toString())));

        verify(itineraryStopRepository).deleteByViaggioId(tripId);
        verify(itineraryStopRepository, never()).deleteByAttivitaIdIn(any());
    }

    @Test
    void eventoDiverso_oIdNonValido_nessunaCancellazione() {
        consumer.handleTripDeletedEvent(new EventDTO<>("TRIP_UPDATED", Map.of("id", tripId.toString())));
        consumer.handleTripDeletedEvent(new EventDTO<>("TRIP_DELETED", Map.of("id", "non-un-uuid")));

        verifyNoInteractions(itineraryStopRepository);
    }
}
