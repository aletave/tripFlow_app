package com.tripflow.catalog_service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Documented
public @interface DateRange {
    String message() default "La data di fine non può essere precedente alla data di inizio";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String startField();
    String endField();

    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        DateRange[] value();
    }
}

