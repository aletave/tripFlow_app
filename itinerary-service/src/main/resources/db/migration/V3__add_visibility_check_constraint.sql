-- Aggiunge vincolo CHECK per la colonna visibility
-- Accetta solo i valori dell'enum Visibility: PUBLIC, PRIVATE, SHARED
ALTER TABLE itineraries
    ADD CONSTRAINT chk_visibility
        CHECK (visibility IN ('PUBLIC', 'PRIVATE', 'SHARED'));