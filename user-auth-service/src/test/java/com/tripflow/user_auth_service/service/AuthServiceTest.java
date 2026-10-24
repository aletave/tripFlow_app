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
import com.tripflow.user_auth_service.model.OtpPurpose;
import com.tripflow.user_auth_service.exception.ResourceNotFoundException;
import com.tripflow.user_auth_service.model.Role;
import com.tripflow.user_auth_service.model.User;
import com.tripflow.user_auth_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private KeycloakAuthClient keycloakAuthClient;

    @Mock
    private UserEventProducer userEventProducer;

    @Mock
    private AvatarStorageConfig avatarStorage;

    @Mock
    private OtpService otpService;

    @Mock
    private MailService mailService;

    @Mock
    private BirthdayCouponService birthdayCouponService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest() {
        return new RegisterRequest("Mario", "Rossi", "mario.rossi@example.com",
                "password", Role.TRAVELER, LocalDate.of(1990, 1, 1), "3331234567");
    }

    //Mock di save che popola l'id come farebbe JPA.
    private void mockSaveConId(UUID id) {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(id);
            return user;
        });
    }

    @Test
    void registerFelice_creaSuKeycloakEpoiSalvaSuDb() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(eq("mario.rossi@example.com"), eq("password"), eq("Mario"), eq("Rossi"),
                eq("TRAVELER"), any())).thenReturn("kc-123");
        mockSaveConId(id);

        RegisterResponse risposta = authService.register(registerRequest());

        assertEquals(id, risposta.id());
        assertEquals("mario.rossi@example.com", risposta.email());
        assertTrue(risposta.requiresLogin());
        assertTrue(risposta.requiresVerification());
        verify(userRepository).existsByEmail("mario.rossi@example.com");
        verify(keycloakAdminClient).createUser(eq("mario.rossi@example.com"), eq("password"), eq("Mario"), eq("Rossi"),
                eq("TRAVELER"), any());
        verify(userRepository).save(any(User.class));
        verify(userEventProducer).publishUserRegistered(id, "kc-123", "mario.rossi@example.com", Role.TRAVELER, "Mario", "Rossi");
        verifyNoMoreInteractions(keycloakAdminClient);
    }

    @Test
    void register_emailGiaPresente_409SenzaToccareKeycloak() {
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(registerRequest()));

        verifyNoInteractions(keycloakAdminClient);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_salvataggioDbFallito_compensazioneEliminaUtenteDaKeycloak() {
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn("kc-123");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB giù"));

        assertThrows(RuntimeException.class, () -> authService.register(registerRequest()));

        verify(keycloakAdminClient, times(1)).deleteUser("kc-123");
    }

    @Test
    void register_salvataggioDbFallito_compensazioneSaltaSeLaRigaEsisteGia() {
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn("kc-123");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB giù"));
        when(userRepository.existsByKeycloakId("kc-123")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.register(registerRequest()));

        //l'account è stato adottato da un'altra registrazione nel frattempo:
        //cancellarlo da Keycloak distruggerebbe un utente legittimo
        verify(keycloakAdminClient, never()).deleteUser(anyString());
    }

    @Test
    void register_emailConMaiuscole_normalizzataInMinuscolo() {
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn("kc-123");
        mockSaveConId(UUID.randomUUID());

        RegisterRequest request = new RegisterRequest("Mario", "Rossi", "Mario.Rossi@Example.COM",
                "password", Role.TRAVELER, LocalDate.of(1990, 1, 1), "3331234567");

        authService.register(request);

        verify(keycloakAdminClient).createUser(eq("mario.rossi@example.com"), eq("password"), eq("Mario"), eq("Rossi"),
                eq("TRAVELER"), any());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("mario.rossi@example.com", captor.getValue().getEmail());
    }

    @Test
    void register_ilPredicateDiAdozioneBocciaGliAccountGiaNelDb() {
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn("kc-123");
        mockSaveConId(UUID.randomUUID());

        authService.register(registerRequest());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Predicate<String>> captor = ArgumentCaptor.forClass((Class) Predicate.class);
        verify(keycloakAdminClient).createUser(anyString(), anyString(), anyString(), anyString(), anyString(),
                captor.capture());

        when(userRepository.existsByKeycloakId("kc-123")).thenReturn(true);
        assertFalse(captor.getValue().test("kc-123"));

        when(userRepository.existsByKeycloakId("kc-999")).thenReturn(false);
        assertTrue(captor.getValue().test("kc-999"));

        //secondo guard: anche se l'account non è nel DB per keycloak_id,
        //un'email appena salvata da una richiesta concorrente blocca l'adozione
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(true);
        assertFalse(captor.getValue().test("kc-999"));
    }

    @Test
    void getMe_utenteTrovato_mappaSuUserResponse() {
        User user = utenteConId(UUID.randomUUID(), "kc-123", "Mario", "Rossi", "mario.rossi@example.com", Role.TRAVELER);
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(user));

        UserResponse risposta = authService.getMe("kc-123");

        assertEquals(user.getId(), risposta.id());
        assertEquals("mario.rossi@example.com", risposta.email());
        assertEquals(Role.TRAVELER, risposta.role());
    }

    @Test
    void getMe_keycloakIdSconosciuto_404() {
        when(userRepository.findByKeycloakId("kc-inesistente")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.getMe("kc-inesistente"));
    }

    @Test
    void register_conDataDiNascita_registraQuandoEStataImpostata() {
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn("kc-123");
        mockSaveConId(UUID.randomUUID());

        authService.register(registerRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertNotNull(captor.getValue().getDateOfBirthSetAt());
    }

    @Test
    void updateProfile_dataDiNascitaGiaImpostata_nonSiPuoCambiare() {
        User user = utenteConId(UUID.randomUUID(), "kc-123");
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(user));

        //cambiarla permetterebbe di far coincidere il compleanno con la prenotazione
        assertThrows(IllegalArgumentException.class, () -> authService.updateProfile("kc-123",
                new UpdateProfileRequest("Mario", "Rossi", LocalDate.of(2001, 6, 1), null)));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_stessaDataONull_nonCambiaNulla() {
        User user = utenteConId(UUID.randomUUID(), "kc-123");
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        authService.updateProfile("kc-123", new UpdateProfileRequest("Mario", "Rossi", LocalDate.of(1990, 1, 1), null));
        authService.updateProfile("kc-123", new UpdateProfileRequest("Mario", "Rossi", null, null));

        assertEquals(LocalDate.of(1990, 1, 1), user.getDateOfBirth());
    }

    @Test
    void updateProfile_primaImpostazioneDellaData_vieneSalvataConLaData() {
        User user = utenteConId(UUID.randomUUID(), "kc-123");
        user.setDateOfBirth(null);
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        authService.updateProfile("kc-123", new UpdateProfileRequest("Mario", "Rossi", LocalDate.of(1995, 3, 3), null));

        assertEquals(LocalDate.of(1995, 3, 3), user.getDateOfBirth());
        assertNotNull(user.getDateOfBirthSetAt());
    }

    @Test
    void changePassword_passwordAttualeCorretta_delegaAKeycloak() {
        User user = utenteConId(UUID.randomUUID(), "kc-123");
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(user));

        authService.changePassword("kc-123", new ChangePasswordRequest("vecchia-password", "nuova-password"));

        //prima verifica la password attuale con un login, poi aggiorna
        verify(keycloakAuthClient).login(user.getEmail(), "vecchia-password");
        verify(keycloakAdminClient).changePassword("kc-123", "nuova-password");
        //tutte le sessioni vengono chiuse: l'app deve rifare il login con la nuova password
        verify(keycloakAdminClient).logoutAllSessions("kc-123");
    }

    @Test
    void changePassword_passwordAttualeErrata_nonCambiaNulla() {
        User user = utenteConId(UUID.randomUUID(), "kc-123");
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(user));
        when(keycloakAuthClient.login(user.getEmail(), "sbagliata"))
                .thenThrow(new InvalidCredentialsException("Email o password non validi"));

        //400 e non 401: un 401 farebbe credere al client che la sessione sia scaduta
        assertThrows(InvalidCurrentPasswordException.class,
                () -> authService.changePassword("kc-123", new ChangePasswordRequest("sbagliata", "nuova-password")));

        verify(keycloakAdminClient, never()).changePassword(anyString(), anyString());
        verify(keycloakAdminClient, never()).logoutAllSessions(anyString());
    }

    @Test
    void deleteMe_primaIlDbPoiKeycloak() {
        User user = utenteConId(UUID.randomUUID(), "kc-123");
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(user));

        authService.deleteMe("kc-123");

        InOrder ordine = inOrder(userRepository, keycloakAdminClient);
        ordine.verify(userRepository).delete(any(User.class));
        ordine.verify(keycloakAdminClient).deleteUser("kc-123");
        verify(userEventProducer).publishUserDeleted(user.getId(), "kc-123", user.getEmail());
        verify(otpService).deleteAllFor("kc-123");
    }

    @Test
    void deleteMe_keycloakGiù_rigaDbEliminataComunque() {
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(utenteConId(UUID.randomUUID(), "kc-123")));
        doThrow(new RuntimeException("Keycloak giù")).when(keycloakAdminClient).deleteUser("kc-123");

        authService.deleteMe("kc-123");

        //il DB è la fonte di verità per l'app: l'utente risulta eliminato,
        //l'eventuale account orfano su Keycloak verrà riadottato alla prossima registrazione
        verify(userRepository).delete(any(User.class));
    }

    @Test
    void getPublicUserByKeycloakId_utenteTrovato_mappaProfiloPubblico() {
        User mario = utenteConId(UUID.randomUUID(), "kc-123", "Mario", "Rossi", "mario@example.com", Role.TRAVELER);
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(mario));

        PublicUserResponse risposta = authService.getPublicUserByKeycloakId("kc-123");

        assertEquals(mario.getId(), risposta.id());
        assertEquals("Mario", risposta.firstName());
        assertEquals("Rossi", risposta.lastName());
        assertEquals(Role.TRAVELER, risposta.role());
    }

    @Test
    void getPublicUserByKeycloakId_keycloakIdSconosciuto_404() {
        when(userRepository.findByKeycloakId("kc-inesistente")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.getPublicUserByKeycloakId("kc-inesistente"));
    }

    @Test
    void loginFelice_restituisceTokenEProfiloUtente() {
        LoginRequest request = new LoginRequest("Mario.Rossi@example.com", "Password123!");
        KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse(
                "mock-access-token", 300L, 1800L, "mock-refresh-token", "Bearer", "openid");
        User user = utenteConId(UUID.randomUUID(), "kc-123", "Mario", "Rossi", "mario.rossi@example.com", Role.TRAVELER);

        when(keycloakAuthClient.login("mario.rossi@example.com", "Password123!")).thenReturn(tokenResponse);
        when(userRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.of(user));
        when(keycloakAdminClient.isEmailVerified("kc-123")).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-access-token", response.accessToken());
        assertEquals("mock-refresh-token", response.refreshToken());
        assertEquals(300L, response.expiresIn());
        assertEquals("Bearer", response.tokenType());
        assertEquals("Mario", response.user().firstName());
        assertEquals("mario.rossi@example.com", response.user().email());
        assertEquals(user.getId(), response.user().id());
    }

    @Test
    void login_credenzialiErrate_rilanciaInvalidCredentialsException() {
        LoginRequest request = new LoginRequest("mario@example.com", "errata");
        when(keycloakAuthClient.login("mario@example.com", "errata"))
                .thenThrow(new InvalidCredentialsException("Email o password non validi"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void login_utenteKeycloakEsisteMaNonNelDb_404() {
        LoginRequest request = new LoginRequest("orfano@example.com", "password");
        KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse(
                "token", 300L, 1800L, "refresh", "Bearer", "openid");

        when(keycloakAuthClient.login("orfano@example.com", "password")).thenReturn(tokenResponse);
        when(userRepository.findByEmail("orfano@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(request));
    }

    @Test
    void refreshFelice_restituisceNuoviToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse(
                "new-access-token", 300L, 1800L, "new-refresh-token", "Bearer", "openid");

        when(keycloakAuthClient.refreshToken("valid-refresh-token")).thenReturn(tokenResponse);

        TokenRefreshResponse response = authService.refresh(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
        assertEquals(300L, response.expiresIn());
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    void refresh_tokenInvalido_rilanciaInvalidCredentialsException() {
        RefreshTokenRequest request = new RefreshTokenRequest("scaduto");
        when(keycloakAuthClient.refreshToken("scaduto"))
                .thenThrow(new InvalidCredentialsException("Refresh token non valido o scaduto"));

        assertThrows(InvalidCredentialsException.class, () -> authService.refresh(request));
    }

    @Test
    void logout_revocaTokenSuKeycloak() {
        LogoutRequest request = new LogoutRequest("valid-refresh-token");
        authService.logout(request);

        verify(keycloakAuthClient).logout("valid-refresh-token");
    }

    // ===== Verifica email con OTP =====

    @Test
    void register_inviaCodiceDiVerificaViaEmail() {
        when(userRepository.existsByEmail("mario.rossi@example.com")).thenReturn(false);
        when(keycloakAdminClient.createUser(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn("kc-123");
        mockSaveConId(UUID.randomUUID());
        when(otpService.issue("kc-123", "mario.rossi@example.com", OtpPurpose.EMAIL_VERIFICATION)).thenReturn("123456");

        authService.register(registerRequest());

        verify(otpService).issue("kc-123", "mario.rossi@example.com", OtpPurpose.EMAIL_VERIFICATION);
        verify(mailService).sendVerificationOtp("mario.rossi@example.com", "Mario", "123456");
    }

    @Test
    void login_emailNonVerificata_rilanciaEmailNotVerifiedException() {
        LoginRequest request = new LoginRequest("mario.rossi@example.com", "Password123!");
        KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse(
                "token", 300L, 1800L, "refresh", "Bearer", "openid");
        User user = utenteConId(UUID.randomUUID(), "kc-123");

        when(keycloakAuthClient.login("mario.rossi@example.com", "Password123!")).thenReturn(tokenResponse);
        when(userRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.of(user));
        when(keycloakAdminClient.isEmailVerified("kc-123")).thenReturn(false);

        assertThrows(EmailNotVerifiedException.class, () -> authService.login(request));
    }

    @Test
    void verifyEmail_codiceCorretto_segnaVerificataSuKeycloak() {
        User user = utenteConId(UUID.randomUUID(), "kc-123");
        when(userRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.of(user));

        authService.verifyEmail(new VerifyEmailRequest("mario.rossi@example.com", "123456"));

        verify(otpService).verify("kc-123", OtpPurpose.EMAIL_VERIFICATION, "123456");
        verify(keycloakAdminClient).markEmailVerified("kc-123");
    }

    @Test
    void verifyEmail_codiceErrato_nonSegnaVerificata() {
        User user = utenteConId(UUID.randomUUID(), "kc-123");
        when(userRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.of(user));
        doThrow(new InvalidOtpException("Codice non valido o scaduto"))
                .when(otpService).verify("kc-123", OtpPurpose.EMAIL_VERIFICATION, "000000");

        assertThrows(InvalidOtpException.class,
                () -> authService.verifyEmail(new VerifyEmailRequest("mario.rossi@example.com", "000000")));

        verify(keycloakAdminClient, never()).markEmailVerified(anyString());
    }

    @Test
    void verifyEmail_emailInesistente_InvalidOtpSenzaVerificare() {
        when(userRepository.findByEmail("ignoto@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidOtpException.class,
                () -> authService.verifyEmail(new VerifyEmailRequest("ignoto@example.com", "123456")));

        verify(otpService, never()).verify(anyString(), any(), anyString());
        verify(keycloakAdminClient, never()).markEmailVerified(anyString());
    }

    @Test
    void resendVerification_giaVerificata_nonInviaNulla() {
        User user = utenteConId(UUID.randomUUID(), "kc-123");
        when(userRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.of(user));
        when(keycloakAdminClient.isEmailVerified("kc-123")).thenReturn(true);

        authService.resendVerification(new ResendVerificationRequest("mario.rossi@example.com"));

        verify(otpService, never()).issue(anyString(), anyString(), any());
        verify(mailService, never()).sendVerificationOtp(anyString(), anyString(), anyString());
    }

    @Test
    void resendVerification_nonVerificata_reinviaIlCodice() {
        User user = utenteConId(UUID.randomUUID(), "kc-123");
        when(userRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.of(user));
        when(keycloakAdminClient.isEmailVerified("kc-123")).thenReturn(false);
        when(otpService.issue("kc-123", "mario.rossi@example.com", OtpPurpose.EMAIL_VERIFICATION)).thenReturn("654321");

        authService.resendVerification(new ResendVerificationRequest("mario.rossi@example.com"));

        verify(mailService).sendVerificationOtp("mario.rossi@example.com", "Mario", "654321");
    }

    // ===== Reset password con OTP =====

    @Test
    void forgotPassword_utenteEsiste_inviaCodiceDiReset() {
        User user = utenteConId(UUID.randomUUID(), "kc-123");
        when(userRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.of(user));
        when(otpService.issue("kc-123", "mario.rossi@example.com", OtpPurpose.PASSWORD_RESET)).thenReturn("111222");

        authService.forgotPassword(new ForgotPasswordRequest("mario.rossi@example.com"));

        verify(mailService).sendPasswordResetOtp("mario.rossi@example.com", "Mario", "111222");
    }

    @Test
    void forgotPassword_utenteNonEsiste_nessunInvio() {
        when(userRepository.findByEmail("ignoto@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(new ForgotPasswordRequest("ignoto@example.com"));

        verify(otpService, never()).issue(anyString(), anyString(), any());
        verify(mailService, never()).sendPasswordResetOtp(anyString(), anyString(), anyString());
    }

    @Test
    void resetPassword_codiceCorretto_aggiornaPasswordSuKeycloak() {
        User user = utenteConId(UUID.randomUUID(), "kc-123");
        when(userRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.of(user));

        authService.resetPassword(new ResetPasswordRequest("mario.rossi@example.com", "123456", "NuovaPassword1!"));

        verify(otpService).verify("kc-123", OtpPurpose.PASSWORD_RESET, "123456");
        verify(keycloakAdminClient).changePassword("kc-123", "NuovaPassword1!");
        verify(keycloakAdminClient).logoutAllSessions("kc-123");
    }

    @Test
    void resetPassword_codiceErrato_nonAggiornaPassword() {
        User user = utenteConId(UUID.randomUUID(), "kc-123");
        when(userRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.of(user));
        doThrow(new InvalidOtpException("Codice non valido o scaduto"))
                .when(otpService).verify("kc-123", OtpPurpose.PASSWORD_RESET, "000000");

        assertThrows(InvalidOtpException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("mario.rossi@example.com", "000000", "NuovaPassword1!")));

        verify(keycloakAdminClient, never()).changePassword(anyString(), anyString());
    }

    @Test
    void resetPassword_emailInesistente_InvalidOtp() {
        when(userRepository.findByEmail("ignoto@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidOtpException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("ignoto@example.com", "123456", "NuovaPassword1!")));

        verify(otpService, never()).verify(anyString(), any(), anyString());
        verify(keycloakAdminClient, never()).changePassword(anyString(), anyString());
    }

    private User utenteConId(UUID id, String keycloakId) {
        return utenteConId(id, keycloakId, "Mario", "Rossi", "mario.rossi@example.com", Role.TRAVELER);
    }

    private User utenteConId(UUID id, String keycloakId, String firstName, String lastName,
                             String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setKeycloakId(keycloakId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setRole(role);
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPhoneNumber("3331234567");
        return user;
    }
}