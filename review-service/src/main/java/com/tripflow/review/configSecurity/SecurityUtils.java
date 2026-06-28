package com.tripflow.review.configSecurity;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Accesso comodo all'identità autenticata presente nel SecurityContext.
 * I controller leggono id e nome del viaggiatore da qui
 * (prima arrivavano dagli header X-Viaggiatore-Id / X-Viaggiatore-Nome).
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UtenteAutenticato utenteCorrente() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UtenteAutenticato utente)) {
            throw new IllegalStateException("Nessun utente autenticato nel contesto di sicurezza");
        }
        return utente;
    }

    public static UUID viaggiatoreId() {
        return utenteCorrente().id();
    }

    public static String nome() {
        return utenteCorrente().nome();
    }
}