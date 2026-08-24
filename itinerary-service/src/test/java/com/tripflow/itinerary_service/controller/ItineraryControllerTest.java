package com.tripflow.itinerary_service.controller;

import com.nimbusds.jose.jwk.RSAKey;
import com.tripflow.itinerary_service.configSecurity.SecurityConfig;
import com.tripflow.itinerary_service.dto.response.ItineraryResponse;
import com.tripflow.itinerary_service.exception.ResourceNotFoundException;
import com.tripflow.itinerary_service.service.ItineraryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItineraryController.class)
@Import({SecurityConfig.class, ItineraryControllerTest.TestJwtConfig.class})
class ItineraryControllerTest {

    //UUID che simula il 'sub' del token emesso da Keycloak
    private static final UUID OWNER_ID = UUID.fromString("70cd15d7-3a55-407c-9909-1f575e86cbfe");
    private static final String OWNER_SUB = OWNER_ID.toString();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItineraryService itineraryService;

    private static final String CREATE_BODY = """
            {
              "title": "Vacanza a Roma",
              "description": "Weekend nel centro storico",
              "visibility": "PRIVATE",
              "startDate": "2026-09-01",
              "endDate": "2026-09-03"
            }
            """;

    private ItineraryResponse rispostaEsempio() {
        return new ItineraryResponse(
                1L, OWNER_ID, "Vacanza a Roma", "Weekend nel centro storico",
                "PRIVATE", null, null, null, null, List.of());
    }

    //Il 'sub' del token finisce come ownerId nel service: niente ownerId dai parametri client
    @Test
    void createItinerary_subDelTokenDiventaOwnerId_201() throws Exception {
        when(itineraryService.createItinerary(any(), eq(OWNER_ID))).thenReturn(rispostaEsempio());

        mockMvc.perform(post("/api/itineraries")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(OWNER_SUB))
                .andExpect(jsonPath("$.title").value("Vacanza a Roma"));

        verify(itineraryService).createItinerary(any(), eq(OWNER_ID));
    }

    //Subject diverso dall'owner dell'itinerario -> il service lancia AccessDeniedException -> 403
    @Test
    void getItineraryById_subjectDiversoDallOwner_403() throws Exception {
        when(itineraryService.getItineraryById(eq(1L), any(UUID.class)))
                .thenThrow(new AccessDeniedException("Itinerary not accessible"));

        mockMvc.perform(get("/api/itineraries/1")
                        .with(jwt().jwt(j -> j.subject("11111111-2222-3333-4444-555555555555"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Itinerary not accessible"));
    }

    @Test
    void getItineraryById_ownerCorretto_200() throws Exception {
        when(itineraryService.getItineraryById(1L, OWNER_ID)).thenReturn(rispostaEsempio());

        mockMvc.perform(get("/api/itineraries/1")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ownerId").value(OWNER_SUB));

        verify(itineraryService).getItineraryById(1L, OWNER_ID);
    }

    @Test
    void getItinerariesByOwner_usaOwnerDalToken_200() throws Exception {
        when(itineraryService.getItinerariesByOwner(OWNER_ID)).thenReturn(List.of(rispostaEsempio()));

        mockMvc.perform(get("/api/itineraries/mie")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ownerId").value(OWNER_SUB));

        verify(itineraryService).getItinerariesByOwner(OWNER_ID);
    }

    @Test
    void deleteItinerary_subjectDiversoDallOwner_403() throws Exception {
        org.mockito.Mockito.doThrow(new AccessDeniedException("Itinerary not accessible"))
                .when(itineraryService).deleteItinerary(eq(1L), any(UUID.class));

        mockMvc.perform(delete("/api/itineraries/1")
                        .with(jwt().jwt(j -> j.subject("11111111-2222-3333-4444-555555555555"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteItinerary_ownerCorretto_200() throws Exception {
        org.mockito.Mockito.doNothing()
                .when(itineraryService).deleteItinerary(1L, OWNER_ID);

        mockMvc.perform(delete("/api/itineraries/1")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB))))
                .andExpect(status().isOk());

        verify(itineraryService).deleteItinerary(1L, OWNER_ID);
    }

    @Test
    void endpointSenzaToken_401() throws Exception {
        mockMvc.perform(get("/api/itineraries/mie"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getItineraryById_inesistente_404() throws Exception {
        when(itineraryService.getItineraryById(eq(99L), any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Itinerary not found with id: 99"));

        mockMvc.perform(get("/api/itineraries/99")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Itinerary not found with id: 99"));
    }

    @Test
    void createItinerary_jsonMalformato_400() throws Exception {
        mockMvc.perform(post("/api/itineraries")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ questo non è JSON valido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo della richiesta non leggibile o JSON malformato"));
    }

    @Test
    void createItinerary_bodyInvalido_400() throws Exception {
        mockMvc.perform(post("/api/itineraries")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "manca il title obbligatorio"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void routeInesistente_404() throws Exception {
        //URL fuori dal contesto del controller: se restasse dentro /api/itineraries/*
        //matcherebbe /{id} e produrrebbe un 400 (type mismatch), non un 404
        mockMvc.perform(get("/api/non-esistente")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Risorsa non trovata: api/non-esistente"));
    }

    @Test
    void erroreInterno_500ConMessaggioGenerico() throws Exception {
        when(itineraryService.getItinerariesByOwner(any(UUID.class)))
                .thenThrow(new RuntimeException("dettaglio interno non esposto"));

        mockMvc.perform(get("/api/itineraries/mie")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Errore interno del server"));
    }

    //JwtDecoder locale con chiave RSA di test: la security chain di SecurityConfig
    //non deve contattare Keycloak/JWKS durante i test.
    @TestConfiguration
    static class TestJwtConfig {

        @Bean
        JwtDecoder jwtDecoder() throws Exception {
            KeyPairGenerator generatore = KeyPairGenerator.getInstance("RSA");
            generatore.initialize(2048);
            KeyPair coppia = generatore.generateKeyPair();
            RSAKey chiave = new RSAKey.Builder((RSAPublicKey) coppia.getPublic()).build();
            return NimbusJwtDecoder.withPublicKey(chiave.toRSAPublicKey()).build();
        }
    }
}
