-- Allineamento a Keycloak: owner_id passa da BIGINT all'UUID del subject del token JWT.
-- Append-only: V1 non viene toccata (i checksum Flyway cambierebbero).
-- NB: se la tabella contiene già righe con owner_id numerici non convertibili in UUID,
-- questa ALTER fallisce; in un progetto di studio il DB è vuoto, altrimenti serve un mapping/refresh.
ALTER TABLE itineraries
    ALTER COLUMN owner_id TYPE UUID USING owner_id::text::uuid;
