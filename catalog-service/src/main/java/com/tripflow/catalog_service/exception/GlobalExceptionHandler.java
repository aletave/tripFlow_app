package com.tripflow.catalog_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        String path = request.getDescription(false).replace("uri=", "");

        ErrorResponseDTO errorResponse = new ErrorResponseDTO();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.NOT_FOUND.value());
        errorResponse.setError(HttpStatus.NOT_FOUND.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(path);

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {

        String path = request.getDescription(false).replace("uri=", "");

        ErrorResponseDTO errorResponse = new ErrorResponseDTO();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.FORBIDDEN.value());
        errorResponse.setError(HttpStatus.FORBIDDEN.getReasonPhrase());
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setPath(path);

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        String path = request.getDescription(false).replace("uri=", "");

        String message = String.format(
                "Il parametro '%s' con valore '%s' non è del tipo atteso: %s",
                ex.getName(),
                ex.getValue(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "sconosciuto"
        );

        ErrorResponseDTO errorResponse = new ErrorResponseDTO();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.setMessage(message);
        errorResponse.setPath(path);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {

        String path = request.getDescription(false).replace("uri=", "");

        ErrorResponseDTO errorResponse = new ErrorResponseDTO();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.setMessage("Il corpo della richiesta non è leggibile o contiene JSON malformato.");
        errorResponse.setPath(path);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> erroriTrovati = new HashMap<>();

        List<ObjectError> listaErrori = ex.getBindingResult().getAllErrors();

        for (int i = 0; i < listaErrori.size(); i++) {

            ObjectError erroreCorrente = listaErrori.get(i);

            FieldError erroreDiCampo = (FieldError) erroreCorrente;

            String nomeCampo = erroreDiCampo.getField();
            String messaggio = erroreDiCampo.getDefaultMessage();

            erroriTrovati.put(nomeCampo, messaggio);
        }

        String stringaErrori = "Errore di validazione: " + erroriTrovati;

        String path = request.getDescription(false).replace("uri=", "");

        ErrorResponseDTO errorResponse = new ErrorResponseDTO();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError(HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.setMessage(stringaErrori);
        errorResponse.setPath(path);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResourceFoundException(
            NoResourceFoundException ex, WebRequest request) {

        String path = request.getDescription(false).replace("uri=", "");

        ErrorResponseDTO errorResponse = new ErrorResponseDTO();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.NOT_FOUND.value());
        errorResponse.setError(HttpStatus.NOT_FOUND.getReasonPhrase());
        errorResponse.setMessage("Risorsa non trovata: " + path);
        errorResponse.setPath(path);

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGlobalException(
            Exception ex, WebRequest request) {

        String path = request.getDescription(false).replace("uri=", "");

        ErrorResponseDTO errorResponse = new ErrorResponseDTO();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        errorResponse.setMessage("Si è verificato un errore imprevisto nel server. Riprova più tardi.");
        errorResponse.setPath(path);

        ex.printStackTrace();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
