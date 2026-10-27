-- Con la nuova semantica PRIVATE = revoca di tutte le condivisioni,
-- eliminiamo le share residue create dalla vecchia logica, che non
-- cancellava le share quando un itinerario tornava privato.
DELETE FROM itinerary_shares
WHERE itinerary_id IN (
    SELECT id
    FROM itineraries
    WHERE visibility = 'PRIVATE'
);
