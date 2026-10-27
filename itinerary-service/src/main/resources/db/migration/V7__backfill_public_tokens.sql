-- Backfill del public_token per gli itinerari già pubblici ma creati prima
-- dell'introduzione della generazione automatica del token in fase di create.
UPDATE itineraries
SET public_token = gen_random_uuid()
WHERE visibility = 'PUBLIC' AND public_token IS NULL;
