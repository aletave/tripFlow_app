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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
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
        when(keycloakAdminClient.createUser(eq("mario.rossi@example.com"), eq("password"), eq("Mario"), eq("Rossi"),
                eq("TRAVELER"), any())).thenReturn("kc-123");
        mockSaveConId(id);

        RegisterResponse risposta = authService.register(registerRequest());

        assertEquals(id, risposta.id());
        assertEquals("mario.rossi@example.com", risposta.email());
        assertTrue(risposta.requiresLogin());
        verify(userRepository).existsByEmail("mario.rossi@example.com");
        verify(keycloakAdminClient).createUser(eq("mario.rossi@example.com"), eq("password"), eq("Mario"), eq("Rossi"),
                eq("TRAVELER"), any());
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
                "password", Role.TRAVELER, LocalDate.of(1990, 1, 1), "3331234567", null);

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
    void changePassword_delegaAKeycloakConIlKeycloakIdDelDb() {
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(utenteConId(UUID.randomUUID(), "kc-123")));

        authService.changePassword("kc-123", new ChangePasswordRequest("nuova-password"));

        verify(keycloakAdminClient).changePassword("kc-123", "nuova-password");
    }

    @Test
    void deleteMe_primaIlDbPoiKeycloak() {
        when(userRepository.findByKeycloakId("kc-123")).thenReturn(Optional.of(utenteConId(UUID.randomUUID(), "kc-123")));

        authService.deleteMe("kc-123");

        InOrder ordine = inOrder(userRepository, keycloakAdminClient);
        ordine.verify(userRepository).delete(any(User.class));
        ordine.verify(keycloakAdminClient).deleteUser("kc-123");
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

    @Test
    void lookupByEmails_emailConMaiuscole_normalizzateETrovate() {
        User mario = utenteConId(UUID.randomUUID(), "kc-1", "Mario", "Rossi", "mario@example.com", Role.TRAVELER);
        when(userRepository.findByEmailIn(List.of("mario@example.com"))).thenReturn(List.of(mario));

        List<PublicUserResponse> risposta = authService.lookupByEmails(List.of("Mario@Example.COM"));

        assertEquals(1, risposta.size());
        assertEquals("Mario", risposta.get(0).firstName());
    }

    @Test
    void lookupByEmails_listaConNullOBlanks_risultatoVuotoSenzaQuery() {
        //Arrays.asList ammette i null, List.of no: serve proprio a simulare input sporchi
        List<PublicUserResponse> risposta = authService.lookupByEmails(Arrays.asList(null, "  ", " "));

        assertTrue(risposta.isEmpty());
        verify(userRepository, never()).findByEmailIn(anyList());
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