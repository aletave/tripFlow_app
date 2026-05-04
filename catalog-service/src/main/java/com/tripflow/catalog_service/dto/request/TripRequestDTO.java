package com.tripflow.catalog_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripRequestDTO {

    @NotBlank(message = "Il nome del viaggio è obbligatorio")
    private String name;

    @NotBlank(message = "La destinazione del viaggio è obbligatoria")
    private String destination;

    @NotNull(message = "La data di inizio del viaggio è obbligatoria")
    private java.time.LocalDate startDate;

    @NotNull(message = "la data di fine del viaggio è obbligatoria")
    private java.time.LocalDate endDate;


    @NotNull(message = "il prezzo del viaggio è obbligatorio")
    @Min(value = 0, message = "Il prezzo del viaggio non può essere negativo")
    private java.math.BigDecimal price;

    @NotNull(message = "il numero di posti disponibili del viaggio è obbligatorio")
    @Min(value = 1, message = "Il numero di posti disponibili del viaggio deve essere > 0")
    private int availableSpots;

    @NotBlank(message = "La descrizione del viaggio è obbligatoria")
    private String description;

    private java.util.List<String> images;



}
