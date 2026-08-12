package com.tripflow.user_auth_service.service;

import com.tripflow.user_auth_service.config.KeycloakAdminClient;
import com.tripflow.user_auth_service.dto.request.ChangePasswordRequest;
import com.tripflow.user_auth_service.dto.request.RegisterRequest;
import com.tripflow.user_auth_service.dto.response.PublicUserResponse;
import com.tripflow.user_auth_service.dto.response.RegisterResponse;
import com.tripflow.user_auth_service.dto.response.UserResponse;
import com.tripflow.user_auth_service.exception.EmailAlreadyExistsException;
import com.tripflow.user_auth_service.exception.ResourceNotFoundException;
import com.tripflow.user_auth_service.model.Role;
import com.tripflow.user_auth_service.model.User;
import com.tripflow.user_auth_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest() {
        return new RegisterRequest("Mario", "Rossi", "mario.rossi@example.com",
                "password", Role.TRAVELER, LocalDate.of(1990, 1, 1), "3331234567", null);
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
        when(keycloakAdminClient.createUser("mario.rossi@example.com", "password", "Mario", "Rossi", "TRAVELER"))
                .thenReturn("kc-123");
        mockSaveConId(id);

        RegisterResponse risposta = authService.register(registerRequest());

        assertEquals(id, risposta.id());
        assertEquals("mario.rossi@example.com", risposta.email());
        assertTrue(risposta.requiresLogin());
        verify(userRepository).existsByEmail("mario.rossi@example.com");
        verify(keycloakAdminClient).createUser("mario.rossi@example.com", "password", "Mario", "Rossi", "TRAVELER");
        verify(userRepository).save(any(User.class));
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
        when(keycloakAdminClient.createUser(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("kc-123");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB giù"));

        assertThrows(RuntimeException.class, () -> authService.register(registerRequest()));

        verify(keycloakAdminClient, times(1)).deleteUser("kc-123");
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
    void changePassword_delegaAKeycloakConIlKeycloakIdDelDb() {
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(utenteConId(UUID.randomUUID(), "kc-123")));

        authService.changePassword("kc-123", new ChangePasswordRequest("vecchia-password", "nuova-password"));

        verify(keycloakAdminClient).changePassword("kc-123", "nuova-password");
    }

    @Test
    void deleteMe_primaKeycloakPoiDb() {
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(utenteConId(UUID.randomUUID(), "kc-123")));

        authService.deleteMe("kc-123");

        verify(keycloakAdminClient).deleteUser("kc-123");
        verify(userRepository).delete(any(User.class));
    }

    @Test
    void lookupByEmails_mappaSoloISuoiProfiloPubblico() {
        User mario = utenteConId(UUID.randomUUID(), "kc-1", "Mario", "Rossi", "mario@example.com", Role.TRAVELER);
        User lucia = utenteConId(UUID.randomUUID(), "kc-2", "Lucia", "Bianchi", "lucia@example.com", Role.ORGANIZER);
        when(userRepository.findByEmailIn(List.of("mario@example.com", "lucia@example.com")))
                .thenReturn(List.of(mario, lucia));

        List<PublicUserResponse> risposta = authService.lookupByEmails(List.of("mario@example.com", "lucia@example.com"));

        assertEquals(2, risposta.size());
        PublicUserResponse primo = risposta.get(0);
        assertEquals("Mario", primo.firstName());
        assertEquals("Rossi", primo.lastName());
        assertEquals(Role.TRAVELER, primo.role());
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