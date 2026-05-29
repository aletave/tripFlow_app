package com.tripflow.catalog_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ActivityRequestDTO {

    @NotBlank(message = "Il nome dell'attività è obbligatorio")
    private String name;

    @NotBlank(message = "La descrizione dell'attività è obbligatoria")
    private String description;

    @NotNull(message = "La durata dell'attività è obbligatoria")
    @Min(value = 1, message = "La durata dell'attività deve essere > 0")
    private Integer duration;

    @NotNull(message = "Il prezzo dell'attività è obbligatorio")
    @Min(value = 0, message = "Il prezzo dell'attività non può essere negativo")
    private BigDecimal price;

    @NotNull(message = "Il numero di posti disponibili dell'attività è obbligatorio")
    @Min(value = 1, message = "Il numero di posti disponibili dell'attività deve essere > 0")
    private Integer availableSpots;

    @NotNull(message = "L'id del viaggio è obbligatorio")
    private UUID tripId;


}
