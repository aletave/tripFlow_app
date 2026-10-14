-- Crea tabella itinerary_shares per la condivisione degli itinerari tra utenti
CREATE TABLE itinerary_shares (
    id BIGSERIAL PRIMARY KEY,

    itinerary_id BIGINT NOT NULL,
    shared_with_user_id UUID NOT NULL,
    can_edit BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_itinerary_shares_itinerary
        FOREIGN KEY (itinerary_id)
            REFERENCES itineraries(id)
            ON DELETE CASCADE,

    CONSTRAINT uk_itinerary_share
        UNIQUE (itinerary_id, shared_with_user_id)
);

-- Indici per query comuni
CREATE INDEX idx_itinerary_shares_itinerary_id ON itinerary_shares(itinerary_id);
CREATE INDEX idx_itinerary_shares_shared_with_user_id ON itinerary_shares(shared_with_user_id);