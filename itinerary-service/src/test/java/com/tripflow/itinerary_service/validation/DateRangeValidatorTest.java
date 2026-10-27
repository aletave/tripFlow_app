package com.tripflow.itinerary_service.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Verifica che @DateRange funzioni sui record (accessor diretto tipo startDate()),
// sui JavaBean (getter getStartDate()) e che una configurazione errata non venga
// piu' ignorata silenziosamente.
class DateRangeValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @DateRange(startField = "startDate", endField = "endDate")
    record DateRangeRecord(LocalDate startDate, LocalDate endDate) {
    }

    @DateRange(startField = "startDatetime", endField = "endDatetime")
    record DateTimeRangeRecord(LocalDateTime startDatetime, LocalDateTime endDatetime) {
    }

    @DateRange(startField = "startDate", endField = "endDate")
    static class DateRangeBean {
        private final LocalDate startDate;
        private final LocalDate endDate;

        DateRangeBean(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }
    }

    @DateRange(startField = "campoInesistente", endField = "altroCampoInesistente")
    record BrokenRecord(LocalDate startDate) {
    }

    @Test
    void record_intervalloValido_nessunaViolazione() {
        Set<ConstraintViolation<DateRangeRecord>> violations = validator.validate(
                new DateRangeRecord(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3)));

        assertThat(violations).isEmpty();
    }

    @Test
    void record_dateUguali_nessunaViolazione() {
        LocalDate giorno = LocalDate.of(2026, 9, 1);

        Set<ConstraintViolation<DateRangeRecord>> violations = validator.validate(
                new DateRangeRecord(giorno, giorno));

        assertThat(violations).isEmpty();
    }

    @Test
    void record_startDopoEnd_violazione() {
        Set<ConstraintViolation<DateRangeRecord>> violations = validator.validate(
                new DateRangeRecord(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1)));

        assertThat(violations)
                .hasSize(1)
                .allSatisfy(v -> {
                    assertThat(v.getMessage()).isEqualTo("La data di fine non puo' essere precedente alla data di inizio");
                    // la violazione e' agganciata al campo di fine, non all'oggetto
                    assertThat(v.getPropertyPath().toString()).isEqualTo("endDate");
                });
    }

    @Test
    void record_dateNulle_nessunaViolazione() {
        Set<ConstraintViolation<DateRangeRecord>> violations = validator.validate(
                new DateRangeRecord(null, null));

        assertThat(violations).isEmpty();
    }

    @Test
    void record_datetime_startDopoEnd_violazione() {
        Set<ConstraintViolation<DateTimeRangeRecord>> violations = validator.validate(
                new DateTimeRangeRecord(
                        LocalDateTime.of(2026, 9, 1, 18, 0),
                        LocalDateTime.of(2026, 9, 1, 9, 0)));

        assertThat(violations).hasSize(1);
    }

    @Test
    void record_datetime_valido_nessunaViolazione() {
        Set<ConstraintViolation<DateTimeRangeRecord>> violations = validator.validate(
                new DateTimeRangeRecord(
                        LocalDateTime.of(2026, 9, 1, 9, 0),
                        LocalDateTime.of(2026, 9, 1, 18, 0)));

        assertThat(violations).isEmpty();
    }

    @Test
    void javaBean_conGetter_funzionaAncora() {
        Set<ConstraintViolation<DateRangeBean>> violations = validator.validate(
                new DateRangeBean(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1)));

        assertThat(violations).hasSize(1);
    }

    @Test
    void accessorInesistente_nonDisattivaIlControlloInSilenzio() {
        assertThatThrownBy(() -> validator.validate(new BrokenRecord(LocalDate.of(2026, 9, 1))))
                .isInstanceOf(ValidationException.class);
    }
}
