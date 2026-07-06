package com.tripflow.booking.data.dto.responses;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoIntentResponse {

    private UUID pagamentoId;
    private String clientSecret;
    private BigDecimal importo;
}