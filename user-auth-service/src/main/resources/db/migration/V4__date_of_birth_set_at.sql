-- =====================================================================
-- V4: data di nascita non modificabile e regole del codice compleanno
--   users.date_of_birth_set_at: quando è stata impostata la data di nascita.
--   Il codice sconto di compleanno vale solo se la data è stata inserita
--   con un certo anticipo rispetto al compleanno (vedi BirthdayCouponService).
-- =====================================================================

ALTER TABLE users ADD COLUMN date_of_birth_set_at TIMESTAMP;

-- per gli account esistenti la data risulta impostata alla registrazione
UPDATE users SET date_of_birth_set_at = created_at WHERE date_of_birth IS NOT NULL;
