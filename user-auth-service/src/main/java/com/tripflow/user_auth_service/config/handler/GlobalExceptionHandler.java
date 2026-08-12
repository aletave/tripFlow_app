package com.tripflow.user_auth_service.config.handler;

import com.tripflow.user_auth_service.dto.errors.ServiceError;
import com.tripflow.user_auth_service.exception.EmailAlreadyExistsException;
import com.tripflow.user_auth_service.exception.KeycloakOperationException;
import com.tripflow.user_auth_service.exception.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Date;
import java.util.stream.Collectors;

//Global Exception Handler per il user-auth-service.

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ServiceError onEntityNotFound(WebRequest req, EntityNotFoundException ex) {
        return errorResponse(req, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ServiceError onResourceNotFound(WebRequest req, ResourceNotFoundException ex) {
        return errorResponse(req, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ServiceError onAccessDenied(WebRequest req, AccessDeniedException ex) {
        return errorResponse(req, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ServiceError onEmailAlreadyExists(WebRequest req, EmailAlreadyExistsException ex) {
        return errorResponse(req, ex.getMessage());
    }

    @ExceptionHandler(KeycloakOperationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ServiceError onKeycloakError(WebRequest req, KeycloakOperationException ex) {
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
    public ServiceError onTypeMismatch(WebRequest req, MethodArgumentTypeMismatchException ex) {
        String tipoAtteso = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "valore valido";
        String message = "Parametro '" + ex.getName() + "' non valido: atteso un " + tipoAtteso;
        return errorResponse(req, message);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ServiceError onMissingHeader(WebRequest req, MissingRequestHeaderException ex) {
        String message = "Header obbligatorio mancante: " + ex.getHeaderName();
        return errorResponse(req, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ServiceError defaultErrorHandler(WebRequest req, Exception ex) {
        log.error("Errore non gestito", ex);
        return errorResponse(req, ex.getMessage());
    }

    private ServiceError errorResponse(WebRequest req, String message) {
        HttpServletRequest httpreq = (HttpServletRequest) req.resolveReference("request");
        ServiceError output = new ServiceError(new Date(), httpreq.getRequestURI(), message);
        log.error("Exception handler :::: {}", output);
        return output;
    }
}