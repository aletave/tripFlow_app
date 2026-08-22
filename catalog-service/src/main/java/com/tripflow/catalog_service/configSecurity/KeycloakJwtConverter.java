package com.tripflow.catalog_service.configSecurity;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String ORGANIZER = "ORGANIZER";
    private static final String TRAVELER = "TRAVELER";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<String> ruoli = ruoliDiRealm(jwt);
        UtenteAutenticato principal = new UtenteAutenticato(
                idUtente(jwt),
                jwt.getClaimAsString("nome"),
                ruoloApplicativo(ruoli));

        List<GrantedAuthority> authorities = ruoli.stream()
                .map(ruolo -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + ruolo))
                .toList();

        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }

    private UUID idUtente(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidBearerTokenException("Il claim 'sub' del token non è un UUID valido");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> ruoliDiRealm(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        return (List<String>) roles;
    }

    private String ruoloApplicativo(List<String> ruoli) {
        if (ruoli.contains(ORGANIZER)) {
            return ORGANIZER;
        }
        return ruoli.contains(TRAVELER) ? TRAVELER : null;
    }
}
