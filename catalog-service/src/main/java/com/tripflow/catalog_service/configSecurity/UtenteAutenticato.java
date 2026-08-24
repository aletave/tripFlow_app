package com.tripflow.catalog_service.configSecurity;

import java.util.UUID;

public record UtenteAutenticato(UUID id, String nome, String ruolo) {
}
