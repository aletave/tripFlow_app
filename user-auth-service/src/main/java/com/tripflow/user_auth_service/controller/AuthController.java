package com.tripflow.user_auth_service.controller;

import com.tripflow.user_auth_service.dto.request.ChangePasswordRequest;
import com.tripflow.user_auth_service.dto.request.RegisterRequest;
import com.tripflow.user_auth_service.dto.request.UpdateProfileRequest;
import com.tripflow.user_auth_service.dto.request.UserLookupRequest;
import com.tripflow.user_auth_service.dto.response.PublicUserResponse;
import com.tripflow.user_auth_service.dto.response.RegisterResponse;
import com.tripflow.user_auth_service.dto.response.UserResponse;
import com.tripflow.user_auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

//Endpoint di registrazione, profilo e lookup utenti.
//Login/refresh/logout NON esistono: avvengono direttamente su Keycloak.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    //Registrazione: crea l'utente su Keycloak e sul DB locale.
    //Il client prosegue poi col login diretto su Keycloak (requiresLogin=true).
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registrazione richiesta per email={}", request.email());
        return authService.register(request);
    }

    //Profilo dell'utente autenticato: il "sub" del JWT Keycloak è il keycloak_id.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(authService.getMe(jwt.getSubject()));
    }

    //Aggiornamento del profilo dell'utente autenticato.
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(authService.updateProfile(jwt.getSubject(), request));
    }

    //Cambio password: la nuova password viene impostata solo su Keycloak.
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        authService.changePassword(jwt.getSubject(), request);
        return ResponseEntity.noContent().build();
    }

    //Eliminazione dell'account: rimuove l'utente da Keycloak e dal DB locale.
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal Jwt jwt) {
        authService.deleteMe(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    //Profilo pubblico di un utente per id (dati minimi: nessuna email/telefono).
    @GetMapping("/users/{id}")
    public ResponseEntity<PublicUserResponse> getPublicUser(@PathVariable UUID id) {
        return ResponseEntity.ok(authService.getPublicUserById(id));
    }

    //Profilo pubblico di più utenti per lista di email.
    @PostMapping("/users/lookup")
    public ResponseEntity<List<PublicUserResponse>> lookupUsers(
            @Valid @RequestBody UserLookupRequest request
    ) {
        return ResponseEntity.ok(authService.lookupByEmails(request.emails()));
    }
}