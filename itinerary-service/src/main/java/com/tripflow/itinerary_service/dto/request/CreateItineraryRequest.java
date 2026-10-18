package com.tripflow.itinerary_service.dto.request;

import com.tripflow.itinerary_service.model.Visibility;
import com.tripflow.itinerary_service.validation.DateRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// Accetta la visibilita' sia come enum esplicito ("visibility": "PUBLIC")
// sia come booleano di comodo ("isPublic": true), cosi' i client piu' vecchi
// non ricevono un 400. Se entrambi assenti, si applica PRIVATE di default.
@DateRange(startField = "startDate", endField = "endDate")
public record CreateItineraryRequest(

        @NotBlank
        @Size(max = 150)
        String title,

        String description,

        Visibility visibility,

        Boolean isPublic,

        LocalDate startDate,

        LocalDate endDate

) {

    public Visibility resolvedVisibility() {
        if (visibility != null) {
            return visibility;
        }
        if (isPublic != null) {
            return isPublic ? Visibility.PUBLIC : Visibility.PRIVATE;
        }
        return Visibility.PRIVATE;
    }
}
