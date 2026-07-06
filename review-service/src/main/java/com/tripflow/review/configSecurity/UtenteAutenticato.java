package com.tripflow.review.configSecurity;

import java.util.UUID;

//JWT firmato:
//id
//nome
//ruolo
public record UtenteAutenticato(UUID id, String nome, String ruolo) {
}