package com.tripflow.booking.data.dto.responses;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoIntentResponse {

    private UUID pagamentoId;      // il nostro id, per correlare lato app
    private String clientSecret;   // serve all'app Android per aprire la PaymentSheet
    private BigDecimal importo;    // per mostrarlo a schermo
}