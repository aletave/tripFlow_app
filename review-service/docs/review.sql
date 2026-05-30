-- =============================================
-- REVIEW SERVICE DATABASE
-- =============================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE tipo_oggetto AS ENUM (
    'VIAGGIO',
    'ATTIVITA'
);

-- =============================================
-- TABELLA RECENSIONE
-- =============================================
CREATE TABLE recensione (
                            id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Riferimenti cross-service (no FK)
                            viaggiatore_id      UUID NOT NULL,
                            prenotazione_id     UUID NOT NULL UNIQUE, -- una sola recensione per prenotazione

    -- Cosa si sta recensendo
                            tipo_oggetto        tipo_oggetto NOT NULL,
                            oggetto_id          UUID NOT NULL, -- viaggio_id o attivita_id

    -- Snapshot per display veloce senza chiamare catalog
                            oggetto_nome_snap   VARCHAR(255) NOT NULL,
                            autore_nome_snap    VARCHAR(255) NOT NULL, -- nome del viaggiatore

    -- Contenuto recensione
                            valutazione         SMALLINT NOT NULL,
                            titolo              VARCHAR(200),
                            commento            TEXT,

    -- Audit
                            created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
                            CONSTRAINT chk_valutazione CHECK (valutazione BETWEEN 1 AND 5),
                            CONSTRAINT uq_viaggiatore_oggetto UNIQUE (viaggiatore_id, oggetto_id)
);

-- =============================================
-- INDICI
-- =============================================
CREATE INDEX idx_recensione_viaggiatore ON recensione(viaggiatore_id);
CREATE INDEX idx_recensione_oggetto     ON recensione(oggetto_id);
CREATE INDEX idx_recensione_tipo        ON recensione(tipo_oggetto);
CREATE INDEX idx_recensione_valutazione ON recensione(valutazione);
CREATE INDEX idx_recensione_data        ON recensione(created_at);