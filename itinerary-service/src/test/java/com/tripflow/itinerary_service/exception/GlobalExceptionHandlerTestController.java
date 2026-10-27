package com.tripflow.itinerary_service.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controller di test isolato: serve a verificare il GlobalExceptionHandler
// senza dipendere da ItineraryController o dal database.
@RestController
@RequestMapping("/test/errors")
public class GlobalExceptionHandlerTestController {

    @GetMapping("/not-found")
    void notFound() {
        throw new ResourceNotFoundException("Itinerary not found with id: 99");
    }

    @GetMapping("/forbidden")
    void forbidden() {
        throw new AccessDeniedException("Only the owner can perform this action");
    }

    @GetMapping("/unauthorized")
    void unauthorized() {
        throw new UnauthorizedActionException("Token non valido");
    }

    @GetMapping("/bad-request")
    void badRequest() {
        throw new IllegalArgumentException("Parametro non valido");
    }

    @GetMapping("/conflict")
    void conflict() {
        throw new IllegalStateException("Operazione non consentita nello stato attuale");
    }

    @GetMapping("/db-conflict")
    void dbConflict() {
        throw new DataIntegrityViolationException("duplicate key value violates unique constraint");
    }

    @PostMapping("/validation")
    void validation(@Valid @RequestBody TestRequest request) {
    }

    @GetMapping("/server-error")
    void serverError() {
        throw new RuntimeException("dettaglio interno non esposto");
    }

    record TestRequest(@NotBlank(message = "name obbligatorio") String name) {
    }
}
