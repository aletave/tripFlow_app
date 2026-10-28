package com.tripflow.catalog_service.validation;

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

        Object startValue = readField(value, startField);
        Object endValue = readField(value, endField);

        // Se uno dei due campi è null, @NotNull se ne occupa separatamente
        if (startValue == null || endValue == null) {
            return true;
        }

        boolean valid;
        if (startValue instanceof LocalDate start && endValue instanceof LocalDate end) {
            valid = !start.isAfter(end);
        } else if (startValue instanceof LocalDateTime start && endValue instanceof LocalDateTime end) {
            valid = !start.isAfter(end);
        } else {
            valid = true;
        }

        if (!valid && context != null) {
            // La violazione viene agganciata a endDate così il client riceve
            // "endDate: <messaggio>" invece di un errore globale sull'oggetto
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(endField)
                    .addConstraintViolation();
        }

        return valid;
    }

    // Supporta sia record accessor (startDate()) sia getter JavaBean (getStartDate())
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
        try {
            return type.getMethod(fieldName);
        } catch (NoSuchMethodException ignored) {
            // prova con getter JavaBean
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

