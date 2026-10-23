package com.tripflow.user_auth_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

//Codice OTP inviato via email per verifica email o reset password.
//Non memorizziamo il codice in chiaro ma solo il suo hash (SHA-256): anche con accesso
//al DB il codice non è leggibile. La sicurezza si regge su hash + scadenza breve +
//numero massimo di tentativi (vedi OtpService).
@Getter
@Setter
@Entity
@Table(name = "email_otp")
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    //Identità dell'utente su Keycloak a cui appartiene il codice.
    @Column(name = "keycloak_id", nullable = false, length = 36)
    private String keycloakId;

    //Email normalizzata (lowercase), ridondante ma utile per diagnostica e indicizzazione.
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private OtpPurpose purpose;

    //Hash SHA-256 (esadecimale) del codice OTP.
    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed", nullable = false)
    private boolean consumed;

    //Tentativi di inserimento falliti: oltre la soglia il codice viene invalidato.
    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
