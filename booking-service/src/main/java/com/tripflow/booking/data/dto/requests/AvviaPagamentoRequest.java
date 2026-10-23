package com.tripflow.booking.data.dto.requests;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvviaPagamentoRequest{

    @Size(max =50, message = "Il codice sconto non può superare i 50 caratteri")
    private String codiceSconto;
}
