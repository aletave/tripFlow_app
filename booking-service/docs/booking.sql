-- BOOKING SERVICE DB
--
-- Documentazione dello schema che Hibernate genera dalle entity JPA
-- Il file serve a leggere la struttura senza dover eseguire tutto

CREATE TABLE prenotazione (
    id UUID NOT NULL,

    viaggiatore_id UUID NOT NULL,
    viaggio_id UUID NOT NULL,

    --i dati viaggio sono snap, congelati al momento della prenotazione
    viaggio_titolo_snap VARCHAR(255) NOT NULL,
    viaggio_destinazione_snap VARCHAR(255) NOT NULL,
    viaggio_data_inizio_snap DATE NOT NULL,
    viaggio_data_fine_snap DATE NOT NULL,
    viaggio_prezzo_snap NUMERIC(10,2) NOT NULL,

    numero_partecipanti INTEGER NOT NULL,
    prezzo_totale NUMERIC(10,2) NOT NULL,
    stato VARCHAR(20) NOT NULL,
    data_prenotazione TIMESTAMP(6) NOT NULL,

    --oltre questo istante l'hold non trattiene piu' i posti
    scadenza_il TIMESTAMP(6),

    note TEXT,

    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,

    version BIGINT NOT NULL,

    CONSTRAINT prenotazione_pkey PRIMARY KEY (id),
    CONSTRAINT prenotazione_stato_check CHECK (stato IN
        ('IN_ATTESA', 'CONFERMATA', 'ANNULLATA', 'COMPLETATA', 'SCADUTA'))
);

CREATE INDEX idx_prenotazione_hold_attivi
    ON prenotazione (viaggio_id, stato, scadenza_il);


CREATE TABLE prenotazione_attivita (
    id UUID NOT NULL,
    prenotazione_id UUID NOT NULL,

    attivita_id UUID NOT NULL,

    --dati attività sempre snap
    attivita_nome_snap VARCHAR(255) NOT NULL,
    attivita_prezzo_snap NUMERIC(10,2) NOT NULL,
    attivita_durata_snap INTEGER NOT NULL,

    created_at TIMESTAMP(6) NOT NULL,

    CONSTRAINT prenotazione_attivita_pkey PRIMARY KEY (id),
    CONSTRAINT uq_prenotazione_attivita UNIQUE (prenotazione_id, attivita_id),
    CONSTRAINT fk_prenotazione_attivita_prenotazione
        FOREIGN KEY (prenotazione_id) REFERENCES prenotazione (id)
);


CREATE TABLE pagamento (
    id UUID NOT NULL,
    prenotazione_id UUID NOT NULL,

    importo NUMERIC(10,2) NOT NULL,
    metodo VARCHAR(20),
    stato VARCHAR(20) NOT NULL,

    --riferimenti per Stripe (non dati sensibili della carta)
    stripe_payment_intent_id VARCHAR(100),
    ultime_quattro_cifre VARCHAR(4),
    brand_carta VARCHAR(20),

    data_pagamento TIMESTAMP(6),

    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,

    version BIGINT NOT NULL,

    CONSTRAINT pagamento_pkey PRIMARY KEY (id),
    CONSTRAINT uk_pagamento_prenotazione UNIQUE (prenotazione_id),
    CONSTRAINT uk_pagamento_stripe_payment_intent UNIQUE (stripe_payment_intent_id),
    CONSTRAINT fk_pagamento_prenotazione
        FOREIGN KEY (prenotazione_id) REFERENCES prenotazione (id),
    CONSTRAINT pagamento_stato_check CHECK (stato IN
        ('IN_ATTESA', 'COMPLETATO', 'FALLITO', 'RIMBORSATO')),
    CONSTRAINT pagamento_metodo_check CHECK (metodo IS NULL OR metodo IN
        ('CARTA_CREDITO', 'CARTA_DEBITO', 'PAYPAL', 'BONIFICO'))
);
