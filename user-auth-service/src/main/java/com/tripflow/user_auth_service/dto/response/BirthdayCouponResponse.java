package com.tripflow.user_auth_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Diritto dell'utente al codice sconto di compleanno nel giorno corrente")
public record BirthdayCouponResponse(
        @Schema(description = "true se oggi l'utente può usare il codice di compleanno")
        boolean eligible,

        @Schema(description = "Codice sconto (Promotion Code Stripe), presente solo se eligible")
        String code,

        @Schema(description = "Primo giorno di validità: il compleanno più recente (o il prossimo, se non ancora arrivato)")
        LocalDate validFrom,

        @Schema(description = "Ultimo giorno di validità, incluso")
        LocalDate validUntil,

        @Schema(description = "Motivo del rifiuto: NO_BIRTH_DATE, OUTSIDE_BIRTHDAY_WINDOW, BIRTH_DATE_TOO_RECENT")
        String reason,

        @Schema(description = "Messaggio leggibile da mostrare all'utente")
        String message
) {
}
