-- Allinea il vincolo stop_type ai payload inviati dall'app: aggiunge 'CUSTOM'.
-- Le tappe CUSTOM sono le tappe inserite manualmente dall'utente (custom_title/
-- custom_description) e non referenziano ne' un viaggio ne' un'attivita'.
-- Per questo va aggiornato anche chk_stop_reference, altrimenti una tappa CUSTOM
-- con viaggio_id/attivita_id nulli verrebbe comunque rifiutata.

ALTER TABLE itinerary_stops
    DROP CONSTRAINT chk_stop_type;

ALTER TABLE itinerary_stops
    ADD CONSTRAINT chk_stop_type
        CHECK (stop_type IN ('TRIP', 'ACTIVITY', 'CUSTOM'));

ALTER TABLE itinerary_stops
    DROP CONSTRAINT chk_stop_reference;

ALTER TABLE itinerary_stops
    ADD CONSTRAINT chk_stop_reference
        CHECK (
            (stop_type = 'TRIP' AND viaggio_id IS NOT NULL AND attivita_id IS NULL)
                OR
            (stop_type = 'ACTIVITY' AND attivita_id IS NOT NULL AND viaggio_id IS NULL)
                OR
            (stop_type = 'CUSTOM' AND viaggio_id IS NULL AND attivita_id IS NULL)
            );
