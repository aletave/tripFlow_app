package com.tripflow.user_auth_service.dto.response;

import java.util.UUID;

//Risposta alla registrazione: nessun token (il login avviene su Keycloak).
//requiresLogin=true comunica al client di procedere col login sull'IdP.
public record RegisterResponse(
        UUID id,
        String email,
        boolean requiresLogin
) {
}