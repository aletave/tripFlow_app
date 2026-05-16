package com.tripflow.review.data.dto.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecensioneUpdateRequest {

    @NotNull(message = "valutazione obbligatoria")
    @Min(value = 1, message = "valutazione minima 1")
    @Max(value = 5, message = "valutazione massima 5")
    private Short valutazione;

    @Size(max = 200, message = "titolo non può superare 200 caratteri")
    private String titolo;

    @Size(max = 5000, message = "commento non può superare i 5000")
    private String commento;
}
