package com.tripflow.user_auth_service.service;

import com.tripflow.user_auth_service.exception.InvalidOtpException;
import com.tripflow.user_auth_service.model.EmailOtp;
import com.tripflow.user_auth_service.model.OtpPurpose;
import com.tripflow.user_auth_service.repository.EmailOtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

//Generazione e verifica dei codici OTP inviati via email.
//Sicurezza: codice a 6 cifre memorizzato solo come hash SHA-256, scadenza breve
//e numero massimo di tentativi. Non è adatto a segreti a lunga vita, ma per un OTP
//monouso e a breve scadenza la combinazione hash + scadenza + tentativi è adeguata.
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final EmailOtpRepository otpRepository;

    @Value("${app.otp.length:6}")
    private int codeLength;

    @Value("${app.otp.expiry-minutes:15}")
    private long expiryMinutes;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    private final SecureRandom secureRandom = new SecureRandom();

    //Genera un nuovo codice, lo salva (come hash) e restituisce il codice in chiaro
    //al chiamante, che lo inoltrerà via email. Eventuali codici precedenti restano a DB
    //ma vengono ignorati: la verifica considera solo il più recente non consumato.
    public String issue(String keycloakId, String email, OtpPurpose purpose) {
        String code = generaCodice();

        EmailOtp otp = new EmailOtp();
        otp.setKeycloakId(keycloakId);
        otp.setEmail(email);
        otp.setPurpose(purpose);
        otp.setCodeHash(hash(code));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        otp.setConsumed(false);
        otp.setAttempts(0);
        otpRepository.save(otp);

        log.info("OTP {} generato per keycloak_id={} (scade tra {} min)", purpose, keycloakId, expiryMinutes);
        return code;
    }

    //Verifica il codice fornito dall'utente. Lancia InvalidOtpException (messaggio generico)
    //se non esiste, è scaduto, ha superato i tentativi o non corrisponde.
    public void verify(String keycloakId, OtpPurpose purpose, String code) {
        EmailOtp otp = otpRepository
                .findFirstByKeycloakIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(keycloakId, purpose)
                .orElseThrow(() -> new InvalidOtpException("Codice non valido o scaduto"));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOtpException("Codice non valido o scaduto");
        }
        if (otp.getAttempts() >= maxAttempts) {
            throw new InvalidOtpException("Codice non valido o scaduto");
        }

        if (!hash(code).equals(otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepository.save(otp);
            throw new InvalidOtpException("Codice non valido o scaduto");
        }

        otp.setConsumed(true);
        otpRepository.save(otp);
        log.info("OTP {} verificato con successo per keycloak_id={}", purpose, keycloakId);
    }

    //Housekeeping: rimuove periodicamente i codici scaduti o già consumati, così la tabella
    //non cresce all'infinito. Cron configurabile, default ogni ora.
    @Transactional
    @Scheduled(cron = "${app.otp.cleanup-cron:0 0 * * * *}")
    public void purgeExpiredAndConsumed() {
        int rimossi = otpRepository.deleteExpiredOrConsumed(LocalDateTime.now());
        if (rimossi > 0) {
            log.info("Pulizia OTP: rimossi {} codici scaduti o consumati", rimossi);
        }
    }

    private String generaCodice() {
        int bound = (int) Math.pow(10, codeLength);
        int value = secureRandom.nextInt(bound);
        return String.format("%0" + codeLength + "d", value);
    }

    private String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            //SHA-256 è garantito dalla JVM: se manca è un errore di ambiente irrecuperabile
            throw new IllegalStateException("Algoritmo SHA-256 non disponibile", e);
        }
    }
}
