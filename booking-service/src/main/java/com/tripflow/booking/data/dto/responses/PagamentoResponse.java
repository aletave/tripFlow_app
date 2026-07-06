package com.tripflow.booking.data.dto.responses;

import com.tripflow.booking.data.entities.enums.MetodoPagamento;
import com.tripflow.booking.data.entities.enums.StatoPagamento;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoResponse {

    private UUID id;
    private BigDecimal importo;
    private MetodoPagamento metodo;
    private StatoPagamento stato;

    //dati per frontend
    private String ultimeQuattroCifre;
    private String brandCarta;

    private LocalDateTime dataPagamento;
    private LocalDateTime createdAt;

    private String stripePaymentIntentId;
}