-- =====================================================================
-- V3: codici OTP via email
--   email_otp: codici monouso per verifica email e reset password.
--              Si memorizza solo l'hash SHA-256 del codice, mai il codice in chiaro.
-- =====================================================================

CREATE TABLE email_otp (
    id           UUID         PRIMARY KEY,
    keycloak_id  VARCHAR(36)  NOT NULL,
    email        VARCHAR(255) NOT NULL,
    purpose      VARCHAR(30)  NOT NULL,
    code_hash    VARCHAR(64)  NOT NULL,
    expires_at   TIMESTAMP    NOT NULL,
    consumed     BOOLEAN      NOT NULL DEFAULT FALSE,
    attempts     INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL,
    CONSTRAINT chk_email_otp_purpose CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);

-- lookup del codice attivo più recente per utente + scopo
CREATE INDEX idx_email_otp_lookup ON email_otp (keycloak_id, purpose, consumed, created_at);
