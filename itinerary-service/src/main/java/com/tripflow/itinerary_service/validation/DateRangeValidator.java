package com.tripflow.itinerary_service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
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

        try {
            Object startValue = getFieldValue(value, startField);
            Object endValue = getFieldValue(value, endField);

            if (startValue == null || endValue == null) {
                return true;
            }

            if (startValue instanceof LocalDate start && endValue instanceof LocalDate end) {
                return !start.isAfter(end);
            }

            if (startValue instanceof LocalDateTime start && endValue instanceof LocalDateTime end) {
                return !start.isAfter(end);
            }

            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private Object getFieldValue(Object obj, String fieldName) throws Exception {
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Method method = obj.getClass().getMethod(getterName);
        return method.invoke(obj);
    }
}