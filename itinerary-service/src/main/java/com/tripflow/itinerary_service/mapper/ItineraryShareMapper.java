package com.tripflow.itinerary_service.mapper;

import com.tripflow.itinerary_service.dto.request.ShareItineraryRequest;
import com.tripflow.itinerary_service.dto.response.ItineraryShareResponse;
import com.tripflow.itinerary_service.model.ItineraryShare;
import org.springframework.stereotype.Component;

@Component
public class ItineraryShareMapper {

    public ItineraryShare toEntity(ShareItineraryRequest request, Long itineraryId) {
        ItineraryShare share = new ItineraryShare();
        share.setItineraryId(itineraryId);
        share.setSharedWithUserId(request.sharedWithUserId());
        share.setCanEdit(request.canEdit());
        return share;
    }

    public ItineraryShareResponse toResponse(ItineraryShare share) {
        return new ItineraryShareResponse(
                share.getId(),
                share.getItineraryId(),
                share.getSharedWithUserId(),
                share.isCanEdit(),
                ItineraryMapper.toUtc(share.getCreatedAt())
        );
    }
}
