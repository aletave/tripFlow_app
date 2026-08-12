-- =====================================================================
-- V2: migrazione a Keycloak
--   users:           + keycloak_id (identità con l'IdP), - password_hash
--   refresh_tokens:  eliminata (i refresh token ora li gestisce Keycloak)
-- =====================================================================

ALTER TABLE users ADD COLUMN keycloak_id VARCHAR(36) UNIQUE;
ALTER TABLE users DROP COLUMN password_hash;
DROP TABLE IF EXISTS refresh_tokens;