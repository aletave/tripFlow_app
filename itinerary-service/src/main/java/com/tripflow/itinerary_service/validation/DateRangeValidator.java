package com.tripflow.itinerary_service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DateRangeValidator implements ConstraintValidator<DateRange, Object> {

    private String startField;
    private String endField;

    @Override
    public void initialize(DateRange constraintAnnotation) {
        this.startField = constraintAnnotation.startField();
        this.endField = constraintAnnotation.endField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        // I DTO di questo servizio sono dei record: espongono accessor diretti
        // (startDate()), non i getter JavaBean (getStartDate()). readField gestisce entrambi.
        Object startValue = readField(value, startField);
        Object endValue = readField(value, endField);

        if (startValue == null || endValue == null) {
            return true;
        }

        boolean valid;
        if (startValue instanceof LocalDate start && endValue instanceof LocalDate end) {
            valid = !start.isAfter(end);
        } else if (startValue instanceof LocalDateTime start && endValue instanceof LocalDateTime end) {
            valid = !start.isAfter(end);
        } else {
            // Tipo non gestito: non blocchiamo la validazione.
            valid = true;
        }

        if (!valid && context != null) {
            // La violazione viene agganciata al campo di fine: il client riceve
            // "endDate : <messaggio>" invece di un errore globale sull'oggetto.
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(endField)
                    .addConstraintViolation();
        }
        return valid;
    }

    // Legge il valore di un campo provando prima l'accessor dei record (nome del campo)
    // e poi il getter JavaBean (get<Nome>). Se nessuno dei due esiste la configurazione
    // di @DateRange e' errata: e' meglio fallire esplicitamente che catturare l'eccezione
    // e restituire true, disattivando di fatto il controllo.
    private Object readField(Object target, String fieldName) {
        Method accessor = findAccessor(target.getClass(), fieldName);
        try {
            return accessor.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new ValidationException(
                    "Impossibile leggere il campo '" + fieldName + "' per la validazione @DateRange", e);
        }
    }

    private Method findAccessor(Class<?> type, String fieldName) {
        // Record: accessor con lo stesso nome del campo (es. startDate()).
        try {
            return type.getMethod(fieldName);
        } catch (NoSuchMethodException ignored) {
            // si prova con il getter JavaBean
        }

        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            return type.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            throw new ValidationException(
                    "Nessun accessor '" + fieldName + "()' o '" + getterName + "()' trovato su "
                            + type.getSimpleName() + " per la validazione @DateRange", e);
        }
    }
}
