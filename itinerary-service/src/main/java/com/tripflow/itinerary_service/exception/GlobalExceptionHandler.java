package com.tripflow.itinerary_service.exception;

import com.tripflow.itinerary_service.dto.errors.ServiceError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Traduce le eccezioni in risposte HTTP con lo stesso formato JSON ({@link ServiceError}).
 *
 * <p>Mappa principale:
 * <ul>
 *   <li>{@code IllegalArgumentException}, validazione, JSON illeggibile: 400;</li>
 *   <li>{@link UnauthorizedActionException}: 401; {@code AccessDeniedException}: 403;</li>
 *   <li>{@link ResourceNotFoundException}, URL inesistente: 404;</li>
 *   <li>{@code IllegalStateException}, vincoli del DB: 409 (o 400 per i dati non validi);</li>
 *   <li>catalog-service o user-auth-service non disponibili: 503;</li>
 *   <li>qualsiasi altro errore: 500 con messaggio generico, dettaglio solo nei log.</li>
 * </ul>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ServiceError> onResourceNotFound(WebRequest req, ResourceNotFoundException ex) {
        return errorResponse(HttpStatus.NOT_FOUND, req, safeMessage(ex.getMessage(), "Risorsa non trovata"), ex);
    }

    @ExceptionHandler(CatalogServiceUnavailableException.class)
    public ResponseEntity<ServiceError> onCatalogUnavailable(WebRequest req, CatalogServiceUnavailableException ex) {
        return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, req,
                safeMessage(ex.getMessage(), "Catalogo non disponibile"), ex);
    }

    @ExceptionHandler(UserAuthServiceUnavailableException.class)
    public ResponseEntity<ServiceError> onUserAuthUnavailable(WebRequest req, UserAuthServiceUnavailableException ex) {
        return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, req,
                safeMessage(ex.getMessage(), "Servizio esterno non disponibile"), ex);
    }

    // Parametri non validi a livello applicativo (es. condivisione duplicata):
    // e' un errore del client, quindi 400 e non 500.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ServiceError> onIllegalArgument(WebRequest req, IllegalArgumentException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST, req, safeMessage(ex.getMessage(), "Richiesta non valida"), ex);
    }

    // Stato applicativo incompatibile con l'operazione richiesta: 409.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ServiceError> onIllegalState(WebRequest req, IllegalStateException ex) {
        return errorResponse(HttpStatus.CONFLICT, req,
                safeMessage(ex.getMessage(), "Operazione non consentita nello stato attuale"), ex);
    }

    // Violazioni di vincoli del database (unicita', foreign key, check...).
    // Il Service valida gia' questi casi; qui resta una rete di sicurezza che
    // traduce i vincoli noti in un messaggio chiaro senza esporre i dettagli del DB:
    // - CHECK su tipo/riferimenti della tappa: dato inviato non valido -> 400
    // - UNIQUE / altri vincoli: conflitto con dati esistenti -> 409
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ServiceError> onDataIntegrityViolation(WebRequest req, DataIntegrityViolationException ex) {
        String detail = String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
        if (detail.contains("chk_stop_type")) {
            return errorResponse(HttpStatus.BAD_REQUEST, req,
                    "stopType non valido. Valori ammessi: TRIP, ACTIVITY, CUSTOM (maiuscolo)", ex);
        }
        if (detail.contains("chk_stop_reference")) {
            return errorResponse(HttpStatus.BAD_REQUEST, req,
                    "Riferimenti della tappa incoerenti: TRIP richiede solo viaggioId, ACTIVITY solo attivitaId, "
                            + "CUSTOM nessuno dei due", ex);
        }
        if (detail.contains("chk_visibility")) {
            return errorResponse(HttpStatus.BAD_REQUEST, req,
                    "visibility non valida. Valori ammessi: PUBLIC, PRIVATE, SHARED", ex);
        }
        if (detail.contains("unique_itinerary_stop_order")) {
            return errorResponse(HttpStatus.CONFLICT, req,
                    "Esiste gia' una tappa con lo stesso stopOrder in questo itinerario", ex);
        }
        if (detail.contains("uk_itinerary_share")) {
            return errorResponse(HttpStatus.CONFLICT, req, "Itinerario gia' condiviso con questo utente", ex);
        }
        return errorResponse(HttpStatus.CONFLICT, req, "Conflitto con i dati esistenti", ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ServiceError> onAccessDenied(WebRequest req, AccessDeniedException ex) {
        return errorResponse(HttpStatus.FORBIDDEN, req, safeMessage(ex.getMessage(), "Accesso negato"), ex);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ServiceError> onUnauthorizedAction(WebRequest req, UnauthorizedActionException ex) {
        return errorResponse(HttpStatus.UNAUTHORIZED, req, safeMessage(ex.getMessage(), "Autenticazione richiesta"), ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ServiceError> onMethodArgumentNotValid(WebRequest req, MethodArgumentNotValidException ex) {
        String message = Stream.concat(
                        ex.getBindingResult().getFieldErrors().stream()
                                .map(viol -> viol.getField() + " : " + viol.getDefaultMessage()),
                        ex.getBindingResult().getGlobalErrors().stream()
                                .map(viol -> viol.getObjectName() + " : " + viol.getDefaultMessage())
                )
                .collect(Collectors.joining(" , "));

        return errorResponse(HttpStatus.BAD_REQUEST, req,
                safeMessage(message, "Richiesta non valida"), ex);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ServiceError> onBodyIlleggibile(WebRequest req, HttpMessageNotReadableException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST, req,
                "Corpo della richiesta non leggibile o JSON malformato", ex);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ServiceError> onTypeMismatch(WebRequest req, MethodArgumentTypeMismatchException ex) {
        String tipoAtteso = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "valore valido";
        String message = "Parametro '" + ex.getName() + "' non valido: atteso un " + tipoAtteso;
        return errorResponse(HttpStatus.BAD_REQUEST, req, message, ex);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ServiceError> onRisorsaNonTrovata(WebRequest req, NoResourceFoundException ex) {
        //URL inesistente: prima finiva sul 500 generico, ma e' un errore del client
        return errorResponse(HttpStatus.NOT_FOUND, req,
                "Risorsa non trovata: " + ex.getResourcePath(), ex);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ServiceError> onMetodoNonSupportato(WebRequest req, HttpRequestMethodNotSupportedException ex) {
        return errorResponse(HttpStatus.METHOD_NOT_ALLOWED, req,
                "Metodo HTTP non supportato: " + ex.getMethod(), ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceError> defaultErrorHandler(WebRequest req, Exception ex) {
        //al client non esponiamo i dettagli interni dell'errore, solo un messaggio generico
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, req, "Errore interno del server", ex);
    }

    private ResponseEntity<ServiceError> errorResponse(
            HttpStatus status,
            WebRequest req,
            String message,
            Exception ex
    ) {
        String path = resolvePath(req);
        ServiceError body = new ServiceError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );

        if (status.is5xxServerError()) {
            log.error("Errore {} su {}: {}", status.value(), path, message, ex);
        } else {
            log.warn("Richiesta rifiutata {} su {}: {}", status.value(), path, message);
        }

        return ResponseEntity.status(status).body(body);
    }

    private String resolvePath(WebRequest req) {
        //fuori dal contesto servlet resolveReference può dare null: si evita l'NPE
        HttpServletRequest httpReq = (HttpServletRequest) req.resolveReference("request");
        return httpReq != null ? httpReq.getRequestURI() : "sconosciuto";
    }

    private String safeMessage(String message, String fallback) {
        return message != null && !message.isBlank() ? message : fallback;
    }
}
