-- =============================================
-- BOOKING SERVICE DATABASE
-- =============================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE stato_prenotazione AS ENUM (
    'IN_ATTESA',
    'CONFERMATA',
    'ANNULLATA',
    'COMPLETATA'
);

CREATE TYPE stato_pagamento AS ENUM (
    'IN_ATTESA',
    'COMPLETATO',
    'FALLITO',
    'RIMBORSATO'
);

CREATE TYPE metodo_pagamento AS ENUM (
    'CARTA_CREDITO',
    'CARTA_DEBITO',
    'PAYPAL',
    'BONIFICO'
);


CREATE TABLE prenotazione (
                              id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Riferimenti cross-service (no FK)
                              viaggiatore_id              UUID NOT NULL,
                              viaggio_id                  UUID NOT NULL,

    -- Snapshot dati viaggio
                              viaggio_titolo_snap         VARCHAR(255) NOT NULL,
                              viaggio_destinazione_snap   VARCHAR(255) NOT NULL,
                              viaggio_data_inizio_snap    DATE NOT NULL,
                              viaggio_data_fine_snap      DATE NOT NULL,
                              viaggio_prezzo_snap         NUMERIC(10,2) NOT NULL,

    -- Dati prenotazione
                              numero_partecipanti         INTEGER NOT NULL DEFAULT 1,
                              prezzo_totale               NUMERIC(10,2) NOT NULL,
                              stato                       stato_prenotazione NOT NULL DEFAULT 'IN_ATTESA',
                              data_prenotazione           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              note                        TEXT,

    -- Audit
                              created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT chk_partecipanti CHECK (numero_partecipanti > 0),
                              CONSTRAINT chk_prezzo_totale CHECK (prezzo_totale >= 0)
);


CREATE TABLE prenotazione_attivita (
                                       id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       prenotazione_id UUID NOT NULL REFERENCES prenotazione(id) ON DELETE CASCADE,

                                       attivita_id     UUID NOT NULL,

    -- Snapshot dati attività
                                       attivita_nome_snap      VARCHAR(255) NOT NULL,
                                       attivita_prezzo_snap    NUMERIC(10,2) NOT NULL,
                                       attivita_durata_snap    INTEGER NOT NULL,

                                       created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT uq_prenotazione_attivita UNIQUE (prenotazione_id, attivita_id)
);


CREATE TABLE pagamento (
                           id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           prenotazione_id             UUID NOT NULL UNIQUE REFERENCES prenotazione(id),

                           importo                     NUMERIC(10,2) NOT NULL,
                           metodo                      metodo_pagamento NOT NULL,
                           stato                       stato_pagamento NOT NULL DEFAULT 'IN_ATTESA',

    -- Riferimenti Stripe
                           stripe_payment_intent_id    VARCHAR(100) UNIQUE,
                           ultime_quattro_cifre        VARCHAR(4),
                           brand_carta                 VARCHAR(20),

                           data_pagamento              TIMESTAMP,

                           created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT chk_importo CHECK (importo > 0)
);


-- INDICI
CREATE INDEX idx_prenotazione_viaggiatore  ON prenotazione(viaggiatore_id);
CREATE INDEX idx_prenotazione_viaggio      ON prenotazione(viaggio_id);
CREATE INDEX idx_prenotazione_stato        ON prenotazione(stato);
CREATE INDEX idx_prenotazione_data         ON prenotazione(data_prenotazione);
CREATE INDEX idx_pren_att_prenotazione     ON prenotazione_attivita(prenotazione_id);
CREATE INDEX idx_pren_att_attivita         ON prenotazione_attivita(attivita_id);
CREATE INDEX idx_pagamento_prenotazione    ON pagamento(prenotazione_id);
CREATE INDEX idx_pagamento_stato           ON pagamento(stato);