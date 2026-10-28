package com.tripflow.catalog_service.dto.request;

import com.tripflow.catalog_service.validation.DateRange;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@DateRange(
        startField = "startDate",
        endField   = "endDate",
        message    = "La data di fine non può essere precedente alla data di inizio"
)
public class TripRequestDTO {

    @NotBlank(message = "Il nome del viaggio è obbligatorio")
    private String name;

    @NotBlank(message = "La destinazione del viaggio è obbligatoria")
    private String destination;

    @NotNull(message = "La data di inizio del viaggio è obbligatoria")
    @FutureOrPresent(message = "La data di inizio non può essere nel passato")
    private LocalDate startDate;

    @NotNull(message = "La data di fine del viaggio è obbligatoria")
    private LocalDate endDate;

    @NotNull(message = "Il prezzo del viaggio è obbligatorio")
    @Min(value = 0, message = "Il prezzo del viaggio non può essere negativo")
    private BigDecimal price;

    @NotNull(message = "Il numero di posti disponibili del viaggio è obbligatorio")
    @Min(value = 1, message = "Il numero di posti disponibili del viaggio deve essere > 0")
    private int availableSpots;

    @NotBlank(message = "La descrizione del viaggio è obbligatoria")
    private String description;

    private List<String> images;
}
