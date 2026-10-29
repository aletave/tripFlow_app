package com.tripflow.itinerary_service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validazione su un DTO: la data di fine non puo' precedere quella di inizio.
 *
 * <p>Funziona con {@code LocalDate} e {@code LocalDateTime}; se una delle due date manca
 * il controllo viene saltato. L'errore viene associato al campo di fine.
 *
 * <pre>{@code @DateRange(startField = "startDate", endField = "endDate")}</pre>
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Documented
public @interface DateRange {
    String message() default "La data di fine non puo' essere precedente alla data di inizio";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /** Nome del campo con la data di inizio. */
    String startField();

    /** Nome del campo con la data di fine. */
    String endField();
}