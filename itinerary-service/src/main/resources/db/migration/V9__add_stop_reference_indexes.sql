-- Indici sui riferimenti al catalogo delle tappe.
-- Servono alla pulizia delle tappe orfane quando il catalogo elimina un viaggio
-- (DELETE ... WHERE viaggio_id = ? / WHERE attivita_id IN (...)), che altrimenti
-- scansionerebbe l'intera tabella. Indici parziali: le colonne sono NULL per
-- le tappe di altro tipo (es. CUSTOM), che non serve indicizzare.
CREATE INDEX IF NOT EXISTS idx_itinerary_stops_viaggio_id
    ON itinerary_stops (viaggio_id)
    WHERE viaggio_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_itinerary_stops_attivita_id
    ON itinerary_stops (attivita_id)
    WHERE attivita_id IS NOT NULL;
