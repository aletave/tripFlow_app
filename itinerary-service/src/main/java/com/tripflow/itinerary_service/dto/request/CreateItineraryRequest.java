package com.tripflow.itinerary_service.dto.request;

import com.tripflow.itinerary_service.model.Visibility;
import com.tripflow.itinerary_service.validation.DateRange;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Dati per creare un itinerario. Le date non possono essere nel passato e la fine
 * non puo' precedere l'inizio.
 *
 * <p>Se {@code visibility} manca l'itinerario e' {@code PRIVATE}.
 */
@DateRange(startField = "startDate", endField = "endDate")
public record CreateItineraryRequest(

        @NotBlank
        @Size(max = 150)
        String title,

        String description,

        Visibility visibility,

        @FutureOrPresent(message = "La data di inizio dell'itinerario non puo' essere nel passato")
        LocalDate startDate,

        @FutureOrPresent(message = "La data di fine dell'itinerario non puo' essere nel passato")
        LocalDate endDate

) {
}
