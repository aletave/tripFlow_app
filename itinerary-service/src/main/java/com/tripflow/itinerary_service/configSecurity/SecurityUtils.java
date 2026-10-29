package com.tripflow.itinerary_service.configSecurity;

import com.tripflow.itinerary_service.exception.UnauthorizedActionException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

/**
 * Accesso all'identita' dell'utente autenticato.
 *
 * <p>Il controller ricava l'utente sempre dal token, mai da parametri inviati dal client:
 * cosi' nessuno puo' agire a nome di un altro.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Restituisce l'id dell'utente corrente, cioe' il claim {@code sub} del JWT.
     *
     * @throws UnauthorizedActionException se manca l'autenticazione o {@code sub} non e' un UUID (401)
     */
    public static UUID ownerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw new UnauthorizedActionException("Nessun utente autenticato nel contesto di sicurezza");
        }
        try {
            return UUID.fromString(jwtAuth.getToken().getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new UnauthorizedActionException("Il claim 'sub' del token non è un UUID valido");
        }
    }
}
