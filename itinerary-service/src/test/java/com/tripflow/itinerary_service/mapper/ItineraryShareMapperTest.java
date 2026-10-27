package com.tripflow.itinerary_service.mapper;

import com.tripflow.itinerary_service.dto.request.ShareItineraryRequest;
import com.tripflow.itinerary_service.dto.response.ItineraryShareResponse;
import com.tripflow.itinerary_service.model.ItineraryShare;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ItineraryShareMapperTest {

    private static final UUID SHARED_USER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private final ItineraryShareMapper mapper = new ItineraryShareMapper();

    @Test
    void toEntity_mappaRequestEItineraryId() {
        ShareItineraryRequest request = new ShareItineraryRequest(SHARED_USER_ID, true);

        ItineraryShare share = mapper.toEntity(request, 1L);

        assertThat(share.getItineraryId()).isEqualTo(1L);
        assertThat(share.getSharedWithUserId()).isEqualTo(SHARED_USER_ID);
        assertThat(share.isCanEdit()).isTrue();
    }

    @Test
    void toResponse_mappaEntity() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 1, 10, 0);
        ItineraryShare share = new ItineraryShare();
        share.setId(10L);
        share.setItineraryId(1L);
        share.setSharedWithUserId(SHARED_USER_ID);
        share.setCanEdit(true);
        share.setCreatedAt(createdAt);

        ItineraryShareResponse response = mapper.toResponse(share);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.itineraryId()).isEqualTo(1L);
        assertThat(response.sharedWithUserId()).isEqualTo(SHARED_USER_ID);
        assertThat(response.canEdit()).isTrue();
        // createdAt e' salvato in UTC e viene esposto con offset esplicito (Z)
        assertThat(response.createdAt()).isEqualTo(createdAt.atOffset(java.time.ZoneOffset.UTC));
        assertThat(response.createdAt().toString()).isEqualTo("2026-10-01T10:00Z");
    }
}
