package com.tripflow.user_auth_service.service;

import com.tripflow.user_auth_service.dto.response.BirthdayCouponResponse;
import com.tripflow.user_auth_service.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class BirthdayCouponServiceTest {

    private BirthdayCouponService service;

    @BeforeEach
    void setUp() {
        service = new BirthdayCouponService();
    }

    @Test
    void ilGiornoDelCompleanno_conDataInseritaDaTempo_codiceValido() {
        oggi(LocalDate.of(2026, 10, 24));

        BirthdayCouponResponse r = service.verifica(utente(LocalDate.of(1999, 10, 24), LocalDate.of(2025, 1, 10)));

        assertTrue(r.eligible());
        assertEquals("BDAY15", r.code());
        assertEquals(LocalDate.of(2026, 10, 24), r.validFrom());
        assertEquals(LocalDate.of(2026, 10, 31), r.validUntil());
    }

    @Test
    void ultimoGiornoDellaFinestra_ancoraValido_ilGiornoDopoNo() {
        User u = utente(LocalDate.of(1999, 10, 24), LocalDate.of(2025, 1, 10));

        oggi(LocalDate.of(2026, 10, 31));
        assertTrue(service.verifica(u).eligible());

        oggi(LocalDate.of(2026, 11, 1));
        BirthdayCouponResponse r = service.verifica(u);
        assertFalse(r.eligible());
        assertNull(r.code());
        assertEquals(BirthdayCouponService.OUTSIDE_BIRTHDAY_WINDOW, r.reason());
        assertEquals(LocalDate.of(2027, 10, 24), r.validFrom());
    }

    @Test
    void primaDelCompleanno_nonValido() {
        oggi(LocalDate.of(2026, 10, 23));

        BirthdayCouponResponse r = service.verifica(utente(LocalDate.of(1999, 10, 24), LocalDate.of(2025, 1, 10)));

        assertFalse(r.eligible());
        assertEquals(BirthdayCouponService.OUTSIDE_BIRTHDAY_WINDOW, r.reason());
    }

    @Test
    void accountCreatoIlGiornoDelCompleanno_codiceSoloDallAnnoProssimo() {
        //l'abuso da evitare: un account nuovo al giorno dichiarando di essere nato quel giorno
        User u = utente(LocalDate.of(1999, 10, 24), LocalDate.of(2026, 10, 24));

        oggi(LocalDate.of(2026, 10, 24));
        BirthdayCouponResponse r = service.verifica(u);
        assertFalse(r.eligible());
        assertEquals(BirthdayCouponService.BIRTH_DATE_TOO_RECENT, r.reason());
        assertEquals(LocalDate.of(2027, 10, 24), r.validFrom());

        oggi(LocalDate.of(2027, 10, 24));
        assertTrue(service.verifica(u).eligible());
    }

    @Test
    void dataInseritaPocoPrimaDelCompleanno_nonValida() {
        //29 giorni prima: sotto la soglia dei 30
        oggi(LocalDate.of(2026, 10, 24));

        assertFalse(service.verifica(utente(LocalDate.of(1999, 10, 24), LocalDate.of(2026, 9, 25))).eligible());
        assertTrue(service.verifica(utente(LocalDate.of(1999, 10, 24), LocalDate.of(2026, 9, 24))).eligible());
    }

    @Test
    void senzaDataDiNascita_nonValido() {
        oggi(LocalDate.of(2026, 10, 24));

        BirthdayCouponResponse r = service.verifica(utente(null, null));

        assertFalse(r.eligible());
        assertEquals(BirthdayCouponService.NO_BIRTH_DATE, r.reason());
    }

    @Test
    void natoIl29Febbraio_annoNonBisestile_festeggiaIl28() {
        User u = utente(LocalDate.of(2000, 2, 29), LocalDate.of(2020, 1, 1));

        oggi(LocalDate.of(2027, 2, 28));
        BirthdayCouponResponse r = service.verifica(u);

        assertTrue(r.eligible());
        assertEquals(LocalDate.of(2027, 2, 28), r.validFrom());
    }

    @Test
    void finestraACavalloDiCapodanno_restaValidaAGennaio() {
        User u = utente(LocalDate.of(1990, 12, 29), LocalDate.of(2020, 1, 1));

        oggi(LocalDate.of(2027, 1, 3));
        BirthdayCouponResponse r = service.verifica(u);

        assertTrue(r.eligible());
        assertEquals(LocalDate.of(2026, 12, 29), r.validFrom());
    }

    @Test
    void ilGiornoSiCalcolaInOraItaliana_nonInUtc() {
        //nei container l'orologio di sistema è UTC: 22:30 UTC è già mezzanotte e mezza a Roma
        User u = utente(LocalDate.of(1999, 10, 24), LocalDate.of(2025, 1, 10));

        istante("2026-10-23T22:30:00Z");
        BirthdayCouponResponse r = service.verifica(u);
        assertTrue(r.eligible());
        assertEquals(LocalDate.of(2026, 10, 24), r.validFrom());

        //ultimo giorno 31/10: alle 00:30 italiane del 1/11 la finestra è chiusa
        istante("2026-10-31T23:30:00Z");
        assertFalse(service.verifica(u).eligible());
    }

    private void istante(String utc) {
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(Instant.parse(utc), ZoneOffset.UTC));
    }

    private void oggi(LocalDate data) {
        Clock clock = Clock.fixed(data.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        ReflectionTestUtils.setField(service, "clock", clock);
    }

    private User utente(LocalDate nascita, LocalDate dataImpostataIl) {
        User user = new User();
        user.setDateOfBirth(nascita);
        user.setDateOfBirthSetAt(dataImpostataIl == null ? null : dataImpostataIl.atTime(LocalDateTime.MIN.toLocalTime()));
        return user;
    }
}
