package com.tripflow.booking.config.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.tripflow.booking.data.dto.errors.ServiceError;
import com.tripflow.booking.exception.BookingException;
import com.tripflow.booking.exception.ServizioNonDisponibileException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.OptimisticLockingFailureException;


import java.util.Date;
import java.util.stream.Collectors;

//Global Exception Handler per il booking-service

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ServiceError onEntityNotFound(WebRequest req, EntityNotFoundException ex) {
        return errorResponse(req, ex.getMessage());
    }


    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ServiceError onAccessDenied(WebRequest req, AccessDeniedException ex) {
        return errorResponse(req, ex.getMessage());
    }

    @ExceptionHandler(BookingException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ServiceError onBookingException(WebRequest req, BookingException ex) {
        return errorResponse(req, ex.getMessage());
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ServiceError onMethodArgumentNotValid(WebRequest req, MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(viol -> viol.getField().concat(" : ")
                        .concat(viol.getDefaultMessage()))
                .collect(Collectors.joining(" , "));
        return errorResponse(req, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ServiceError onTypeMismatch(
            WebRequest req,
            MethodArgumentTypeMismatchException ex) {
        String tipoAtteso;
        if (ex.getRequiredType() != null) {
            tipoAtteso = ex.getRequiredType().getSimpleName();
        } else {
            tipoAtteso = "valore valido";
        }
        String message = "Parametro '" + ex.getName()
                + "' non valido: atteso un " + tipoAtteso;
        return errorResponse(req, message);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ServiceError onMissingHeader(WebRequest req, MissingRequestHeaderException ex) {
        String message = "Header obbligatorio mancante: " + ex.getHeaderName();
        return errorResponse(req, message);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ServiceError onConflittoDiConcorrenza(WebRequest req, OptimisticLockingFailureException ex) {
        log.warn("Conflitto di concorrenza, la risorsa e' stata modificata da un'altra operazione", ex);
        return errorResponse(req,
                "La prenotazione e' stata modificata da un'altra operazione. Ricarica e riprova.");
    }

    @ExceptionHandler(ServizioNonDisponibileException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ServiceError onServizioNonDisponibile(WebRequest req, ServizioNonDisponibileException ex) {
        return errorResponse(req, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ServiceError onDataIntegrityViolation(WebRequest req, DataIntegrityViolationException ex) {
        return errorResponse(req, "Operazione già in corso o già eseguita. Ricarica e riprova.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ServiceError onMessageNotReadable(WebRequest req, HttpMessageNotReadableException ex) {
        String message = "Body della richiesta non valido";
        if (ex.getCause() instanceof JsonMappingException jme && !jme.getPath().isEmpty()) {
            message = "Valore non valido per il campo '" + jme.getPath().get(0).getFieldName() + "'";
        }
        return errorResponse(req, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ServiceError onMissingParameter(WebRequest req, MissingServletRequestParameterException ex) {
        return errorResponse(req, "Parametro obbligatorio mancante: " + ex.getParameterName());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ServiceError onNoResource(WebRequest req, NoResourceFoundException ex) {
        return errorResponse(req, "Endpoint non trovato");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ServiceError onMethodNotSupported(WebRequest req, HttpRequestMethodNotSupportedException ex) {
        return errorResponse(req, "Metodo " + ex.getMethod() + " non supportato su questo endpoint");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ServiceError defaultErrorHandler(WebRequest req, Exception ex) {
        log.error("Errore non gestito", ex);
        return errorResponse(req, "Errore interno del server");
    }

    private ServiceError errorResponse(WebRequest req, String message) {
        HttpServletRequest httpreq = (HttpServletRequest) req.resolveReference("request");
        ServiceError output = new ServiceError(new Date(), httpreq.getRequestURI(), message);
        log.error("Exception handler :::: {}", output);
        return output;
    }
}