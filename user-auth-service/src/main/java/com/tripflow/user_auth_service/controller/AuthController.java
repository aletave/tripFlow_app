package com.tripflow.user_auth_service.controller;

import com.tripflow.user_auth_service.config.AvatarStorageConfig;
import com.tripflow.user_auth_service.dto.errors.ServiceError;
import com.tripflow.user_auth_service.dto.request.ChangePasswordRequest;
import com.tripflow.user_auth_service.dto.request.ForgotPasswordRequest;
import com.tripflow.user_auth_service.dto.request.LoginRequest;
import com.tripflow.user_auth_service.dto.request.LogoutRequest;
import com.tripflow.user_auth_service.dto.request.RefreshTokenRequest;
import com.tripflow.user_auth_service.dto.request.RegisterRequest;
import com.tripflow.user_auth_service.dto.request.ResendVerificationRequest;
import com.tripflow.user_auth_service.dto.request.ResetPasswordRequest;
import com.tripflow.user_auth_service.dto.request.UpdateProfileRequest;
import com.tripflow.user_auth_service.dto.request.VerifyEmailRequest;
import com.tripflow.user_auth_service.dto.request.UserLookupByKeycloakIdsRequest;
import com.tripflow.user_auth_service.dto.response.BirthdayCouponResponse;
import com.tripflow.user_auth_service.dto.response.LoginResponse;
import com.tripflow.user_auth_service.dto.response.PublicUserResponse;
import com.tripflow.user_auth_service.dto.response.RegisterResponse;
import com.tripflow.user_auth_service.dto.response.TokenRefreshResponse;
import com.tripflow.user_auth_service.dto.response.UserResponse;
import com.tripflow.user_auth_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller REST per registrazione, autenticazione (proxy verso Keycloak),
 * gestione del profilo utente e lookup utenti per altri microservizi.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication & User Profile", description = "API per registrazione, autenticazione (proxy Keycloak), gestione del profilo utente e lookup")
public class AuthController {

    private final AuthService authService;
    private final AvatarStorageConfig avatarStorage;

    @Operation(summary = "Registrazione nuovo utente",
            description = "Crea l'account su Keycloak con le credenziali fornite e memorizza il profilo applicativo su DB. Pubblica l'evento USER_REGISTERED su RabbitMQ.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utente registrato con successo"),
            @ApiResponse(responseCode = "400", description = "Dati di registrazione non validi",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "409", description = "Email già registrata",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "429", description = "Troppe richieste (Rate limit superato)",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registrazione richiesta per email={}", request.email());
        return authService.register(request);
    }

    @Operation(summary = "Login utente",
            description = "Autentica l'utente tramite Keycloak (Direct Access Grants) e restituisce access token, refresh token e il profilo utente completo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticazione riuscita"),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida / campi mancanti",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "401", description = "Credenziali non valide",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "404", description = "Profilo utente locale non trovato",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "429", description = "Troppe richieste (Rate limit superato)",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Richiesta di login per email={}", request.email());
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Rinnovo access token",
            description = "Rinnova l'access token scaduto scambiando un refresh token valido rilasciato in precedenza da Keycloak.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token rinnovato con successo"),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token scaduto o non valido",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(summary = "Logout utente",
            description = "Revoca il refresh token tramite l'endpoint standard di revoca RFC 7009 su Keycloak.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout eseguito con successo"),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida / token mancante",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "500", description = "Errore durante la revoca del token",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        log.info("Richiesta di logout e revoca token");
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Profilo dell'utente autenticato",
            description = "Restituisce i dati anagrafici e di contatto dell'utente attualmente autenticato (individuato dal claim 'sub' del JWT).",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profilo recuperato con successo"),
            @ApiResponse(responseCode = "401", description = "Non autenticato o token non valido",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "404", description = "Profilo utente non trovato",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(authService.getMe(jwt.getSubject()));
    }

    @Operation(summary = "Aggiornamento profilo utente",
            description = "Aggiorna i dati del profilo dell'utente autenticato e notifica l'evento USER_UPDATED su RabbitMQ.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profilo aggiornato con successo"),
            @ApiResponse(responseCode = "400", description = "Dati del profilo non validi o tentativo di modificare la data di nascita",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "401", description = "Non autenticato",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "404", description = "Utente non trovato",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(authService.updateProfile(jwt.getSubject(), request));
    }

    @Operation(summary = "Diritto al codice sconto di compleanno",
            description = "Indica se oggi l'utente autenticato può usare il codice sconto di compleanno: "
                    + "vale dal compleanno ai giorni successivi configurati, e solo se la data di nascita "
                    + "è stata inserita con anticipo sufficiente. Chiamato da booking-service al checkout.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Esito della verifica (eligible true/false con motivo)"),
            @ApiResponse(responseCode = "401", description = "Non autenticato",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "404", description = "Utente non trovato",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @GetMapping("/me/birthday-coupon")
    public ResponseEntity<BirthdayCouponResponse> getBirthdayCoupon(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(authService.getBirthdayCoupon(jwt.getSubject()));
    }

    @Operation(summary = "Cambio password",
            description = "Verifica la password attuale, aggiorna la password su Keycloak e chiude tutte le sessioni "
                    + "dell'utente: i refresh token esistenti smettono di funzionare e serve un nuovo login.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password modificata con successo"),
            @ApiResponse(responseCode = "400", description = "Password attuale errata o nuova password non valida",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "401", description = "Non autenticato",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        authService.changePassword(jwt.getSubject(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Caricamento avatar",
            description = "Carica una nuova immagine del profilo, elimina il file precedente e aggiorna il profilo con la nuova URL.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Immagine caricata con successo"),
            @ApiResponse(responseCode = "400", description = "File non valido o formato non supportato",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "401", description = "Non autenticato",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @Parameter(description = "File immagine (JPEG, PNG, WebP)")
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt
    ) throws IOException {
        UserResponse current = authService.getMe(jwt.getSubject());
        String oldImage = current.profileImage();

        String filename = avatarStorage.salva(file);
        // FIX: URL relativa al Gateway — evita che Android (o Docker) riceva un URL
        // con localhost:8086 che punta a se stesso invece che al backend.
        // Il client concatena la baseUrl del Gateway (es. http://10.0.2.2:8080)
        // + /user-auth-service/api/auth/avatars/{filename}.
        String url = "/user-auth-service/api/auth/avatars/" + filename;

        authService.updateProfileImage(jwt.getSubject(), url);
        avatarStorage.elimina(oldImage);

        log.info("Avatar aggiornato per keycloak_id={}: {}", jwt.getSubject(), url);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @Operation(summary = "Serving immagine profilo",
            description = "Restituisce l'immagine avatar dal disco locale (endpoint pubblico).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File immagine restituito"),
            @ApiResponse(responseCode = "404", description = "File non trovato")
    })
    @GetMapping("/avatars/{filename}")
    public ResponseEntity<Resource> serveAvatar(
            @Parameter(description = "Nome file dell'avatar salvato")
            @PathVariable String filename
    ) {
        Path file = avatarStorage.percorsoAvatar(filename);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = filename.endsWith(".png") ? "image/png"
                    : filename.endsWith(".webp") ? "image/webp"
                    : "image/jpeg";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Eliminazione account",
            description = "Elimina l'account utente dal database locale e da Keycloak, e notifica l'evento USER_DELETED su RabbitMQ.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account eliminato con successo"),
            @ApiResponse(responseCode = "401", description = "Non autenticato",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal Jwt jwt) {
        authService.deleteMe(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Profilo pubblico per Keycloak ID",
            description = "Restituisce i dati pubblici essenziali (id, nome, cognome, ruolo, avatar) cercati per Keycloak ID (claim 'sub').",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profilo pubblico recuperato"),
            @ApiResponse(responseCode = "401", description = "Non autenticato",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "404", description = "Utente non trovato",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @GetMapping("/users/by-keycloak/{keycloakId}")
    public ResponseEntity<PublicUserResponse> getPublicUserByKeycloakId(
            @Parameter(description = "Identificativo Keycloak (sub)")
            @PathVariable String keycloakId
    ) {
        return ResponseEntity.ok(authService.getPublicUserByKeycloakId(keycloakId));
    }

    @Operation(summary = "Profilo pubblico per ID utente",
            description = "Restituisce i dati pubblici essenziali cercati tramite UUID locale.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profilo pubblico recuperato"),
            @ApiResponse(responseCode = "401", description = "Non autenticato",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "404", description = "Utente non trovato",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @GetMapping("/users/{id}")
    public ResponseEntity<PublicUserResponse> getPublicUser(
            @Parameter(description = "UUID del record utente")
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(authService.getPublicUserById(id));
    }

    @Operation(summary = "Lookup batch profili pubblici per Keycloak ID (sub del JWT)",
            description = "Risolve in un colpo solo i profili pubblici di una lista di Keycloak ID. " +
                    "Utilizzato dai microservizi che salvano il sub del JWT (es. booking, review) " +
                    "per eliminare il problema N+1 nelle schermate con liste di autori.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista profili pubblici restituita"),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida",
                    content = @Content(schema = @Schema(implementation = ServiceError.class))),
            @ApiResponse(responseCode = "401", description = "Non autenticato",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @PostMapping("/users/lookup-by-ids")
    public ResponseEntity<List<PublicUserResponse>> lookupUsersByKeycloakIds(
            @Valid @RequestBody UserLookupByKeycloakIdsRequest request
    ) {
        return ResponseEntity.ok(authService.lookupByKeycloakIds(request.keycloakIds()));
    }

    @Operation(summary = "Recupero password dimenticata",
            description = "Invia una mail con il link di reset password tramite Keycloak (server SMTP configurato nel realm). " +
                    "Per sicurezza risponde sempre 202 Accepted, indipendentemente dal fatto che l'email esista.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Richiesta accettata (la mail è inviata se l'email esiste)"),
            @ApiResponse(responseCode = "400", description = "Email non valida",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Reset password con codice OTP",
            description = "Completa il recupero password: valida il codice OTP ricevuto via mail e imposta la nuova password.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password reimpostata con successo"),
            @ApiResponse(responseCode = "400", description = "Codice non valido/scaduto o password non valida",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Verifica email con codice OTP",
            description = "Verifica l'indirizzo email inserendo il codice OTP ricevuto durante la registrazione. "
                    + "Sblocca il login.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Email verificata con successo"),
            @ApiResponse(responseCode = "400", description = "Codice non valido o scaduto",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reinvio codice di verifica email",
            description = "Reinvia un nuovo codice OTP di verifica. Per sicurezza risponde sempre 202, "
                    + "indipendentemente dal fatto che l'email esista o sia già verificata.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Richiesta accettata (il codice è inviato se l'email esiste e non è verificata)"),
            @ApiResponse(responseCode = "400", description = "Email non valida",
                    content = @Content(schema = @Schema(implementation = ServiceError.class)))
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request);
        return ResponseEntity.accepted().build();
    }
}