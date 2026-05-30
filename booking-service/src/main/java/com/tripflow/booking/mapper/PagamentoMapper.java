package com.tripflow.booking.mapper;

import com.tripflow.booking.data.dto.responses.PagamentoResponse;
import com.tripflow.booking.data.entities.Pagamento;

public final class PagamentoMapper {

    private PagamentoMapper() {
    }

    public static PagamentoResponse toResponse(Pagamento p) {
        if (p == null) return null;
        return PagamentoResponse.builder()
                .id(p.getId())
                .importo(p.getImporto())
                .metodo(p.getMetodo())
                .stato(p.getStato())
                .stripePaymentIntentId(p.getStripePaymentIntentId())
                .ultimeQuattroCifre(p.getUltimeQuattroCifre())
                .brandCarta(p.getBrandCarta())
                .dataPagamento(p.getDataPagamento())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
