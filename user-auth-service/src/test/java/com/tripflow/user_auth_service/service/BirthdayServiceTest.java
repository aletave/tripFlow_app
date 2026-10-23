package com.tripflow.user_auth_service.service;

import com.tripflow.user_auth_service.model.Role;
import com.tripflow.user_auth_service.model.User;
import com.tripflow.user_auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BirthdayServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    @InjectMocks
    private BirthdayService birthdayService;

    @BeforeEach
    void setUp() {
        //data neutra di default (non 28/02): i test base non devono dipendere dal giorno reale
        fissaData(LocalDate.of(2025, 6, 15));
    }

    @Test
    void inviaAuguri_inviaUnaMailPerOgniFesteggiato() {
        User mario = utente("mario@example.com", "Mario");
        User lucia = utente("lucia@example.com", "Lucia");
        when(userRepository.findByBirthday(anyInt(), anyInt())).thenReturn(List.of(mario, lucia));

        birthdayService.inviaAuguriDiCompleanno();

        verify(mailService).sendBirthdayGreeting(eq("mario@example.com"), eq("Mario"), anyString());
        verify(mailService).sendBirthdayGreeting(eq("lucia@example.com"), eq("Lucia"), anyString());
    }

    @Test
    void inviaAuguri_nessunCompleanno_nessunInvio() {
        when(userRepository.findByBirthday(anyInt(), anyInt())).thenReturn(List.of());

        birthdayService.inviaAuguriDiCompleanno();

        verify(mailService, never()).sendBirthdayGreeting(anyString(), anyString(), anyString());
    }

    @Test
    void inviaAuguri_unInvioFallisce_continuaConGliAltri() {
        User mario = utente("mario@example.com", "Mario");
        User lucia = utente("lucia@example.com", "Lucia");
        when(userRepository.findByBirthday(anyInt(), anyInt())).thenReturn(List.of(mario, lucia));
        doThrow(new RuntimeException("SMTP giù"))
                .when(mailService).sendBirthdayGreeting(eq("mario@example.com"), anyString(), anyString());

        birthdayService.inviaAuguriDiCompleanno();

        //nonostante il fallimento sul primo utente, il secondo riceve comunque gli auguri
        verify(mailService).sendBirthdayGreeting(eq("lucia@example.com"), eq("Lucia"), anyString());
    }

    @Test
    void inviaAuguri_28FebbraioNonBisestile_includeNatiIl29() {
        //28/02/2025: anno non bisestile → i nati il 29/02 festeggiano oggi
        fissaData(LocalDate.of(2025, 2, 28));
        when(userRepository.findByBirthday(2, 28)).thenReturn(List.of(utente("mario@example.com", "Mario")));
        when(userRepository.findByBirthday(2, 29)).thenReturn(List.of(utente("lucia@example.com", "Lucia")));

        birthdayService.inviaAuguriDiCompleanno();

        verify(mailService).sendBirthdayGreeting(eq("mario@example.com"), eq("Mario"), anyString());
        verify(mailService).sendBirthdayGreeting(eq("lucia@example.com"), eq("Lucia"), anyString());
    }

    @Test
    void inviaAuguri_28FebbraioBisestile_nonCercaIl29() {
        //28/02/2024: anno bisestile → il 29/02 esiste, quindi oggi NON si anticipa nulla
        fissaData(LocalDate.of(2024, 2, 28));
        when(userRepository.findByBirthday(2, 28)).thenReturn(List.of(utente("mario@example.com", "Mario")));

        birthdayService.inviaAuguriDiCompleanno();

        verify(userRepository, never()).findByBirthday(2, 29);
        verify(mailService).sendBirthdayGreeting(eq("mario@example.com"), eq("Mario"), anyString());
    }

    private void fissaData(LocalDate data) {
        Clock clock = Clock.fixed(data.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        ReflectionTestUtils.setField(birthdayService, "clock", clock);
    }

    private User utente(String email, String firstName) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setKeycloakId("kc-" + firstName);
        user.setFirstName(firstName);
        user.setLastName("Rossi");
        user.setEmail(email);
        user.setRole(Role.TRAVELER);
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        return user;
    }
}
