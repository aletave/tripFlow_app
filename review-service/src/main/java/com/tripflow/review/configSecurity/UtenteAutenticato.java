package com.tripflow.review.configSecurity;

import java.util.UUID;

/**
 * Identità autenticata ricavata dal JWT firmato:
 * - id    = subject del token (id viaggiatore)
 * - nome  = claim "nome" (display name, usato come autore della recensione)
 * - ruolo = claim "role"
 *
 * È il principal che {@link JwtAuthenticationFilter} mette nel SecurityContext.
 */
public record UtenteAutenticato(UUID id, String nome, String ruolo) {
}