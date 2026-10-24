package com.tripflow.user_auth_service.service;

import com.tripflow.user_auth_service.config.AvatarStorageConfig;
import com.tripflow.user_auth_service.config.KeycloakAdminClient;
import com.tripflow.user_auth_service.config.KeycloakAuthClient;
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
import com.tripflow.user_auth_service.dto.response.BirthdayCouponResponse;
import com.tripflow.user_auth_service.dto.response.KeycloakTokenResponse;
import com.tripflow.user_auth_service.dto.response.LoginResponse;
import com.tripflow.user_auth_service.dto.response.PublicUserResponse;
import com.tripflow.user_auth_service.dto.response.RegisterResponse;
import com.tripflow.user_auth_service.dto.response.TokenRefreshResponse;
import com.tripflow.user_auth_service.dto.response.UserResponse;
import com.tripflow.user_auth_service.exception.EmailAlreadyExistsException;
import com.tripflow.user_auth_service.exception.EmailNotVerifiedException;
import com.tripflow.user_auth_service.exception.InvalidCredentialsException;
import com.tripflow.user_auth_service.exception.InvalidCurrentPasswordException;
import com.tripflow.user_auth_service.exception.InvalidOtpException;
import com.tripflow.user_auth_service.exception.ResourceNotFoundException;
import com.tripflow.user_auth_service.model.OtpPurpose;
import com.tripflow.user_auth_service.model.User;
import com.tripflow.user_auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

//Service di autenticazione e gestione profilo.
//Coordina la registrazione e il login (proxy verso Keycloak) arricchendo le risposte
//con i dati di profilo memorizzati nel DB locale.
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final KeycloakAdminClient keycloakAdminClient;
    private final KeycloakAuthClient keycloakAuthClient;
    private final UserEventProducer userEventProducer;
    private final AvatarStorageConfig avatarStorage;
    private final OtpService otpService;
    private final MailService mailService;
    private final BirthdayCouponService birthdayCouponService;

    //Registrazione con scrittura doppia (dual-write) in ordine DB-check -> Keycloak -> DB-save.
    //Se il salvataggio su DB fallisce, compensa cancellando l'utente da Keycloak (con 1 retry).
    public RegisterResponse register(RegisterRequest request) {
        //email normalizzata: Keycloak è case-insensitive, il DB no — evita doppioni con maiuscole
        String email = request.email().toLowerCase(Locale.ROOT);

        //1. controllo su DB: email già registrata -> 409, STOP
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        //2. creazione su Keycloak (+ assegnazione ruolo); su 409 il client adotta l'account
        //orfano solo se non esiste già nel DB locale (predicate). Si ricontrolla anche l'email:
        //nel frattempo un'altra registrazione concorrente potrebbe aver salvato proprio quell'utente
        String keycloakId = keycloakAdminClient.createUser(
                email,
                request.password(),
                request.firstName(),
                request.lastName(),
                request.role().name(),
                id -> !userRepository.existsByKeycloakId(id) && !userRepository.existsByEmail(email));

        //3. salvataggio su DB, con compensazione in caso di errore
        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(email);
        user.setRole(request.role());
        user.setDateOfBirth(request.dateOfBirth());
        if (request.dateOfBirth() != null) {
            user.setDateOfBirthSetAt(LocalDateTime.now());
        }
        user.setPhoneNumber(request.phoneNumber());

        try {
            userRepository.save(user);
        } catch (RuntimeException e) {
            compensaUtenteOrfano(keycloakId);
            throw e;
        }

        userEventProducer.publishUserRegistered(
                user.getId(),
                user.getKeycloakId(),
                user.getEmail(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName()
        );

        //invio del codice OTP di verifica email: un eventuale errore d'invio non deve far
        //fallire la registrazione (l'utente potrà richiedere un nuovo codice con /resend-verification)
        inviaCodiceVerifica(user);

        log.info("Utente registrato: id={}, email={}", user.getId(), user.getEmail());
        return new RegisterResponse(user.getId(), user.getEmail(), true, true);
    }

    //Genera e invia via email il codice OTP di verifica dell'indirizzo email.
    private void inviaCodiceVerifica(User user) {
        try {
            String codice = otpService.issue(user.getKeycloakId(), user.getEmail(), OtpPurpose.EMAIL_VERIFICATION);
            mailService.sendVerificationOtp(user.getEmail(), user.getFirstName(), codice);
        } catch (RuntimeException e) {
            log.error("Invio del codice di verifica fallito per email={}", user.getEmail(), e);
        }
    }

    //Autenticazione utente (proxy Keycloak): valida le credenziali, ottiene i token JWT
    //e arricchisce la risposta con il profilo completo presente nel database locale.
    public LoginResponse login(LoginRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);

        // 1. Scambio credenziali con Keycloak via Direct Access Grants
        KeycloakTokenResponse tokenResponse = keycloakAuthClient.login(email, request.password());

        // 2. Recupero del profilo applicativo associato all'email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Profilo utente non trovato per l'email: " + email));

        // 3. Blocco dell'accesso se l'email non è ancora stata verificata via OTP.
        // Per i nuovi account lo blocca già Keycloak (azione VERIFY_EMAIL, vedi KeycloakAuthClient);
        // il controllo resta per gli account creati prima di quella modifica.
        if (!keycloakAdminClient.isEmailVerified(user.getKeycloakId())) {
            log.warn("Login bloccato: email non verificata per email={}", email);
            throw new EmailNotVerifiedException(
                    "Email non verificata. Inserisci il codice che ti abbiamo inviato per mail.");
        }

        log.info("Login riuscito per email={}, userId={}", email, user.getId());
        return new LoginResponse(
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                tokenResponse.expiresIn(),
                tokenResponse.tokenType() != null ? tokenResponse.tokenType() : "Bearer",
                toUserResponse(user)
        );
    }

    //Rinnovo del token JWT scaduto tramite refresh_token su Keycloak.
    public TokenRefreshResponse refresh(RefreshTokenRequest request) {
        KeycloakTokenResponse tokenResponse = keycloakAuthClient.refreshToken(request.refreshToken());
        return new TokenRefreshResponse(
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                tokenResponse.expiresIn(),
                tokenResponse.tokenType() != null ? tokenResponse.tokenType() : "Bearer"
        );
    }

    //Logout con revoca RFC 7009 del refresh token su Keycloak.
    public void logout(LogoutRequest request) {
        keycloakAuthClient.logout(request.refreshToken());
        log.info("Logout completato con revoca del token");
    }

    //Profilo dell'utente autenticato: lookup per keycloak_id (il "sub" del JWT Keycloak).
    public UserResponse getMe(String keycloakId) {
        return toUserResponse(loadByKeycloakId(keycloakId));
    }

    //Aggiornamento della sola immagine profilo: usato dall'endpoint di upload avatar.
    public void updateProfileImage(String keycloakId, String imageUrl) {
        User user = loadByKeycloakId(keycloakId);
        user.setProfileImage(imageUrl);
        User saved = userRepository.save(user);
        userEventProducer.publishUserProfileUpdated(saved.getId(), saved.getKeycloakId(),
                saved.getFirstName(), saved.getLastName(), saved.getProfileImage());
        log.info("Immagine profilo aggiornata per keycloak_id={}", keycloakId);
    }

    //Aggiornamento del profilo (solo campi anagrafici: le credenziali sono su Keycloak).
    //L'immagine profilo si cambia solo da /me/avatar: un URL scelto dal client finirebbe
    //poi in AvatarStorageConfig.elimina() e permetterebbe di cancellare file altrui.
    public UserResponse updateProfile(String keycloakId, UpdateProfileRequest request) {
        User user = loadByKeycloakId(keycloakId);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        aggiornaDataDiNascita(user, request.dateOfBirth());
        user.setPhoneNumber(request.phoneNumber());

        User saved = userRepository.save(user);
        userEventProducer.publishUserProfileUpdated(saved.getId(), saved.getKeycloakId(),
                saved.getFirstName(), saved.getLastName(), saved.getProfileImage());
        log.info("Profilo aggiornato per keycloak_id={}", keycloakId);
        return toUserResponse(saved);
    }

    //Cambio password: verifica prima la password attuale (tentando un login su Keycloak),
    //poi aggiorna la credenziale su Keycloak e chiude tutte le sessioni dell'utente,
    //compresa quella del dispositivo corrente: si rientra con la nuova password.
    public void changePassword(String keycloakId, ChangePasswordRequest request) {
        User user = loadByKeycloakId(keycloakId);

        //conferma dell'identità: la password attuale deve essere corretta
        try {
            keycloakAuthClient.login(user.getEmail(), request.currentPassword());
        } catch (InvalidCredentialsException e) {
            throw new InvalidCurrentPasswordException("La password attuale non è corretta");
        }

        keycloakAdminClient.changePassword(user.getKeycloakId(), request.newPassword());
        //chiude anche la sessione aperta dal login di verifica qui sopra
        keycloakAdminClient.logoutAllSessions(user.getKeycloakId());
        log.info("Password aggiornata per keycloak_id={}", keycloakId);
    }

    //Eliminazione account: prima il DB locale, poi Keycloak, infine evento asincrono RabbitMQ
    //per notificare gli altri microservizi (es. booking, catalog, itinerary) della cancellazione.
    public void deleteMe(String keycloakId) {
        User user = loadByKeycloakId(keycloakId);
        UUID userId = user.getId();
        String email = user.getEmail();
        String profileImage = user.getProfileImage();

        userRepository.delete(user);
        //i codici OTP contengono l'email: non vanno lasciati dopo la cancellazione
        otpService.deleteAllFor(keycloakId);
        if (avatarStorage != null && profileImage != null) {
            avatarStorage.elimina(profileImage);
        }
        try {
            keycloakAdminClient.deleteUser(user.getKeycloakId());
        } catch (RuntimeException e) {
            log.warn("Account locale eliminato, ma la cancellazione su Keycloak è fallita per {}", keycloakId, e);
        }

        userEventProducer.publishUserDeleted(userId, keycloakId, email);
        log.info("Account eliminato: keycloak_id={}", keycloakId);
    }

    //Recupero del profilo pubblico di un utente per keycloak_id (il "sub" del JWT Keycloak).
    //Usato dagli altri servizi per risolvere l'utente di una prenotazione/recensione:
    //salvano viaggiatore_id = sub, quindi il lookup per id DB locale non li copre.
    public PublicUserResponse getPublicUserByKeycloakId(String keycloakId) {
        return toPublicUserResponse(loadByKeycloakId(keycloakId));
    }

    //Recupero del profilo pubblico di un utente per id DB.
    public PublicUserResponse getPublicUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato: " + id));
        return toPublicUserResponse(user);
    }

    //Lookup batch per Keycloak ID (il "sub" del JWT): usato dai microservizi per risolvere
    //in un colpo solo tutti gli autori di una lista di prenotazioni/recensioni/itinerari.
    //Sostituisce le N chiamate singole a /users/by-keycloak/{id} (problema N+1).
    public List<PublicUserResponse> lookupByKeycloakIds(List<String> keycloakIds) {
        List<String> idsFiltrati = keycloakIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (idsFiltrati.isEmpty()) {
            return List.of();
        }
        return userRepository.findByKeycloakIdIn(idsFiltrati)
                .stream()
                .map(this::toPublicUserResponse)
                .toList();
    }

    //Verifica dell'email tramite codice OTP: valida il codice e, se corretto,
    //segna l'email come verificata su Keycloak sbloccando il login.
    public void verifyEmail(VerifyEmailRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        //messaggio generico per non rivelare se l'email esiste (anti-enumeration)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidOtpException("Codice non valido o scaduto"));

        otpService.verify(user.getKeycloakId(), OtpPurpose.EMAIL_VERIFICATION, request.code());
        keycloakAdminClient.markEmailVerified(user.getKeycloakId());
        log.info("Email verificata per email={}", email);
    }

    //Reinvio del codice di verifica email. Per sicurezza risponde sempre con successo:
    //non fa nulla se l'email non esiste o è già verificata.
    public void resendVerification(ResendVerificationRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        userRepository.findByEmail(email).ifPresent(user -> {
            if (keycloakAdminClient.isEmailVerified(user.getKeycloakId())) {
                log.info("Reinvio verifica saltato: email già verificata per email={}", email);
                return;
            }
            inviaCodiceVerifica(user);
        });
    }

    //Recupero password (passo 1): cerca l'utente per email e, se esiste, invia via mail
    //un codice OTP per il reset. Per sicurezza non rivela se l'email esiste (niente user
    //enumeration): l'endpoint risponde sempre 202 anche se l'email non è registrata.
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        userRepository.findByEmail(email).ifPresent(user -> {
            try {
                String codice = otpService.issue(user.getKeycloakId(), user.getEmail(), OtpPurpose.PASSWORD_RESET);
                mailService.sendPasswordResetOtp(user.getEmail(), user.getFirstName(), codice);
                log.info("Codice di reset password inviato per email={}", email);
            } catch (RuntimeException e) {
                log.error("Errore durante l'invio del codice di reset per email={}", email, e);
                //non propaghiamo: la risposta al client è sempre 202 per sicurezza
            }
        });
    }

    //Recupero password (passo 2): valida il codice OTP e, se corretto, imposta la nuova
    //password su Keycloak. Messaggio generico in caso di email inesistente (anti-enumeration).
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidOtpException("Codice non valido o scaduto"));

        otpService.verify(user.getKeycloakId(), OtpPurpose.PASSWORD_RESET, request.code());
        keycloakAdminClient.changePassword(user.getKeycloakId(), request.newPassword());
        //chi ha resettato la password potrebbe averlo fatto perché l'account era compromesso
        keycloakAdminClient.logoutAllSessions(user.getKeycloakId());
        log.info("Password reimpostata via OTP per email={}", email);
    }

    //La data di nascita si imposta una volta sola: cambiarla permetterebbe di far coincidere
    //il compleanno con il giorno della prenotazione e ottenere il codice sconto a piacere.
    //Null nella richiesta = nessuna modifica (un client può non inviarla).
    private void aggiornaDataDiNascita(User user, LocalDate nuovaData) {
        if (nuovaData == null || nuovaData.equals(user.getDateOfBirth())) {
            return;
        }
        if (user.getDateOfBirth() != null) {
            throw new IllegalArgumentException("La data di nascita non può essere modificata");
        }
        user.setDateOfBirth(nuovaData);
        user.setDateOfBirthSetAt(LocalDateTime.now());
    }

    //Verifica se l'utente ha diritto oggi al codice sconto di compleanno.
    //Usato da booking-service al checkout quando viene inserito il codice di compleanno.
    public BirthdayCouponResponse getBirthdayCoupon(String keycloakId) {
        return birthdayCouponService.verifica(loadByKeycloakId(keycloakId));
    }

    private User loadByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utente non trovato con keycloak_id: " + keycloakId));
    }

    //Se il salvataggio su DB fallisce dopo la creazione su Keycloak, elimina l'utente
    //dall'IdP per non lasciare orfani: retry 1x, poi log dell'eventuale residuo.
    private void compensaUtenteOrfano(String keycloakId) {
        //guard: se nel frattempo un'altra richiesta ha salvato la riga per questo account,
        //non è più un orfano — cancellarlo da Keycloak distruggerebbe un utente legittimo
        if (userRepository.existsByKeycloakId(keycloakId)) {
            log.warn("Compensazione saltata: l'utente {} esiste già nel DB locale", keycloakId);
            return;
        }
        try {
            keycloakAdminClient.deleteUser(keycloakId);
            log.info("Compensazione riuscita: utente {} rimosso da Keycloak", keycloakId);
        } catch (RuntimeException e) {
            log.error("Compensazione fallita per {}, retry...", keycloakId, e);
            try {
                keycloakAdminClient.deleteUser(keycloakId);
            } catch (RuntimeException e2) {
                log.error("Utente orfano su Keycloak: {}", keycloakId, e2);
            }
        }
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getDateOfBirth(),
                user.getPhoneNumber(),
                user.getProfileImage());
    }

    private PublicUserResponse toPublicUserResponse(User user) {
        return new PublicUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getProfileImage());
    }
}