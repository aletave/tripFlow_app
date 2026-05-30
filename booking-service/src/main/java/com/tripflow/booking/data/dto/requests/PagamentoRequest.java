package com.tripflow.booking.data.dto.requests;

import com.tripflow.booking.data.entities.enums.MetodoPagamento;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoRequest {


    @NotNull(message = "L'importo è obbligatorio")
    @Positive(message = "L'importo deve essere maggiore di zero")
    private BigDecimal importo;

    @NotNull(message = "Il metodo di pagamento è obbligatorio")
    private MetodoPagamento metodo;

    //per stripe
    private String stripePaymentIntentId;
}