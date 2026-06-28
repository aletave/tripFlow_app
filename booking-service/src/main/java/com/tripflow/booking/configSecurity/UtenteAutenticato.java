package com.tripflow.booking.configSecurity;

import java.util.UUID;

public record UtenteAutenticato(UUID id, String nome, String ruolo) {
}