package com.tripflow.itinerary_service.configSecurity;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

//Helper per estrarre l'identita' dell'utente dal token JWT nel SecurityContextHolder.
//Il controller lo usa al posto dei parametri client: l'owner e' sempre il subject del token.
public final class SecurityUtils {

    private SecurityUtils() {
    }

    //Estrae UUID.fromString(jwt.getSubject()) dall'Authentication corrente.
    public static UUID ownerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("Nessun utente autenticato nel contesto di sicurezza");
        }
        try {
            return UUID.fromString(jwtAuth.getToken().getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalStateException("Il claim 'sub' del token non è un UUID valido");
        }
    }
}
