package com.tripflow.user_auth_service.service;

import com.tripflow.user_auth_service.exception.InvalidOtpException;
import com.tripflow.user_auth_service.model.EmailOtp;
import com.tripflow.user_auth_service.model.OtpPurpose;
import com.tripflow.user_auth_service.repository.EmailOtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private EmailOtpRepository otpRepository;

    @InjectMocks
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        //i campi @Value non vengono iniettati fuori dal contesto Spring: li impostiamo a mano
        ReflectionTestUtils.setField(otpService, "codeLength", 6);
        ReflectionTestUtils.setField(otpService, "expiryMinutes", 15L);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 5);
    }

    @Test
    void issue_generaCodiceA6CifreEMemorizzaSoloLHash() {
        String code = otpService.issue("kc-1", "a@b.it", OtpPurpose.EMAIL_VERIFICATION);

        assertTrue(code.matches("\\d{6}"), "il codice deve essere di 6 cifre");

        ArgumentCaptor<EmailOtp> captor = ArgumentCaptor.forClass(EmailOtp.class);
        verify(otpRepository).save(captor.capture());
        EmailOtp salvato = captor.getValue();

        assertEquals("kc-1", salvato.getKeycloakId());
        assertEquals(OtpPurpose.EMAIL_VERIFICATION, salvato.getPurpose());
        assertNotEquals(code, salvato.getCodeHash(), "il codice non deve essere salvato in chiaro");
        assertEquals(sha256(code), salvato.getCodeHash());
        assertFalse(salvato.isConsumed());
        assertEquals(0, salvato.getAttempts());
        assertTrue(salvato.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void issue_invalidaICodiciPrecedentiPrimaDiSalvareIlNuovo() {
        otpService.issue("kc-1", "a@b.it", OtpPurpose.PASSWORD_RESET);

        //altrimenti un codice vecchio tornerebbe valido dopo l'uso di quello nuovo
        InOrder ordine = inOrder(otpRepository);
        ordine.verify(otpRepository).invalidateActive("kc-1", OtpPurpose.PASSWORD_RESET);
        ordine.verify(otpRepository).save(any(EmailOtp.class));
    }

    @Test
    void verify_codiceCorretto_consumaIlCodice() {
        EmailOtp otp = otpValido("123456");
        when(otpRepository.findFirstByKeycloakIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                "kc-1", OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.of(otp));

        otpService.verify("kc-1", OtpPurpose.PASSWORD_RESET, "123456");

        assertTrue(otp.isConsumed());
        verify(otpRepository).save(otp);
    }

    @Test
    void verify_codiceErrato_incrementaTentativiELancia() {
        EmailOtp otp = otpValido("123456");
        when(otpRepository.findFirstByKeycloakIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                "kc-1", OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.of(otp));

        assertThrows(InvalidOtpException.class,
                () -> otpService.verify("kc-1", OtpPurpose.PASSWORD_RESET, "999999"));

        assertEquals(1, otp.getAttempts());
        assertFalse(otp.isConsumed());
        verify(otpRepository).save(otp);
    }

    @Test
    void verify_codiceScaduto_lancia() {
        EmailOtp otp = otpValido("123456");
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(otpRepository.findFirstByKeycloakIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                "kc-1", OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.of(otp));

        assertThrows(InvalidOtpException.class,
                () -> otpService.verify("kc-1", OtpPurpose.PASSWORD_RESET, "123456"));
    }

    @Test
    void verify_troppiTentativi_lancia() {
        EmailOtp otp = otpValido("123456");
        otp.setAttempts(5);
        when(otpRepository.findFirstByKeycloakIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                "kc-1", OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.of(otp));

        assertThrows(InvalidOtpException.class,
                () -> otpService.verify("kc-1", OtpPurpose.PASSWORD_RESET, "123456"));
    }

    @Test
    void verify_nessunCodicePresente_lancia() {
        when(otpRepository.findFirstByKeycloakIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                "kc-1", OtpPurpose.PASSWORD_RESET)).thenReturn(Optional.empty());

        assertThrows(InvalidOtpException.class,
                () -> otpService.verify("kc-1", OtpPurpose.PASSWORD_RESET, "123456"));
    }

    @Test
    void purge_delegaLaCancellazioneAlRepository() {
        when(otpRepository.deleteExpiredOrConsumed(any())).thenReturn(4);

        otpService.purgeExpiredAndConsumed();

        verify(otpRepository).deleteExpiredOrConsumed(any());
    }

    private EmailOtp otpValido(String code) {
        EmailOtp otp = new EmailOtp();
        otp.setKeycloakId("kc-1");
        otp.setEmail("a@b.it");
        otp.setPurpose(OtpPurpose.PASSWORD_RESET);
        otp.setCodeHash(sha256(code));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        otp.setConsumed(false);
        otp.setAttempts(0);
        return otp;
    }

    private String sha256(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
