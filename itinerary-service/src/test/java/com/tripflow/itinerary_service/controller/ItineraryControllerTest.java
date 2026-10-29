package com.tripflow.itinerary_service.controller;

import com.nimbusds.jose.jwk.RSAKey;
import com.tripflow.itinerary_service.configSecurity.SecurityConfig;
import com.tripflow.itinerary_service.dto.response.ItineraryResponse;
import com.tripflow.itinerary_service.dto.response.ItineraryShareResponse;
import com.tripflow.itinerary_service.exception.ResourceNotFoundException;
import com.tripflow.itinerary_service.exception.UserAuthServiceUnavailableException;
import com.tripflow.itinerary_service.model.Visibility;
import com.tripflow.itinerary_service.service.ItineraryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItineraryController.class)
@Import({SecurityConfig.class, ItineraryControllerTest.TestJwtConfig.class})
class ItineraryControllerTest {

    private static final UUID OWNER_ID = UUID.fromString("70cd15d7-3a55-407c-9909-1f575e86cbfe");
    private static final String OWNER_SUB = OWNER_ID.toString();
    private static final UUID SHARED_USER_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String SHARED_USER_SUB = SHARED_USER_ID.toString();
    private static final UUID STRANGER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String STRANGER_SUB = STRANGER_ID.toString();
    private static final UUID PUBLIC_TOKEN = UUID.fromString("12345678-1234-1234-1234-123456789012");
    private static final UUID PRIVATE_TOKEN = UUID.fromString("87654321-4321-4321-4321-210987654321");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItineraryService itineraryService;

    private static final String CREATE_BODY = """
            {
              "title": "Vacanza a Roma",
              "description": "Weekend nel centro storico",
              "visibility": "PRIVATE",
              "startDate": "2030-09-01",
              "endDate": "2030-09-03"
            }
            """;

    private ItineraryResponse privateItinerary() {
        return new ItineraryResponse(
                1L, OWNER_ID, "Vacanza a Roma", "Weekend nel centro storico",
                Visibility.PRIVATE, null, null, null, null, List.of(), null, false);
    }

    private ItineraryResponse publicItinerary() {
        return new ItineraryResponse(
                2L, OWNER_ID, "Itinerario Pubblico", "Visibile a tutti",
                Visibility.PUBLIC, null, null, null, null, List.of(), PUBLIC_TOKEN, false);
    }

    private ItineraryResponse sharedItinerary() {
        return new ItineraryResponse(
                3L, OWNER_ID, "Itinerario Condiviso", "Condiviso con utente",
                Visibility.SHARED, null, null, null, null, List.of(), null, false);
    }

    @Test
    void createItinerary_subDelTokenDiventaOwnerId_201() throws Exception {
        when(itineraryService.createItinerary(any(), eq(OWNER_ID))).thenReturn(privateItinerary());

        mockMvc.perform(post("/api/itineraries")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(OWNER_SUB))
                .andExpect(jsonPath("$.title").value("Vacanza a Roma"));

        verify(itineraryService).createItinerary(any(), eq(OWNER_ID));
    }

    @Test
    void getItineraryById_ownerCorretto_200() throws Exception {
        when(itineraryService.getItineraryById(1L, OWNER_ID)).thenReturn(privateItinerary());

        mockMvc.perform(get("/api/itineraries/1")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ownerId").value(OWNER_SUB));

        verify(itineraryService).getItineraryById(1L, OWNER_ID);
    }

    @Test
    void getItineraryById_sharedUserWithReadAccess_200() throws Exception {
        when(itineraryService.getItineraryById(3L, SHARED_USER_ID)).thenReturn(sharedItinerary());

        mockMvc.perform(get("/api/itineraries/3")
                        .with(jwt().jwt(j -> j.subject(SHARED_USER_SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.ownerId").value(OWNER_SUB))
                // il FE legge canEdit per mostrare/nascondere i pulsanti di modifica
                .andExpect(jsonPath("$.canEdit").value(false));

        verify(itineraryService).getItineraryById(3L, SHARED_USER_ID);
    }

    @Test
    void getItineraryById_publicVisibility_200() throws Exception {
        when(itineraryService.getItineraryById(2L, STRANGER_ID)).thenReturn(publicItinerary());

        mockMvc.perform(get("/api/itineraries/2")
                        .with(jwt().jwt(j -> j.subject(STRANGER_SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));

        verify(itineraryService).getItineraryById(2L, STRANGER_ID);
    }

    @Test
    void getItineraryById_subjectDiversoDallOwnerPrivate_403() throws Exception {
        when(itineraryService.getItineraryById(eq(1L), any(UUID.class)))
                .thenThrow(new AccessDeniedException("Itinerary not accessible"));

        mockMvc.perform(get("/api/itineraries/1")
                        .with(jwt().jwt(j -> j.subject(STRANGER_SUB))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Itinerary not accessible"));
    }

    @Test
    void getPublicItinerary_tokenValido_200() throws Exception {
        when(itineraryService.getPublicItinerary(PUBLIC_TOKEN)).thenReturn(
                new com.tripflow.itinerary_service.dto.response.PublicItineraryResponse(
                        2L, "Itinerario Pubblico", "Visibile a tutti", null, null, List.of()));

        mockMvc.perform(get("/api/itineraries/public/" + PUBLIC_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("Itinerario Pubblico"))
                // il link pubblico non deve esporre dati sull'owner o sui permessi
                .andExpect(jsonPath("$.ownerId").doesNotExist())
                .andExpect(jsonPath("$.canEdit").doesNotExist())
                .andExpect(jsonPath("$.publicToken").doesNotExist());

        verify(itineraryService).getPublicItinerary(PUBLIC_TOKEN);
    }

    @Test
    void getPublicItinerary_tokenNonPubblico_404() throws Exception {
        when(itineraryService.getPublicItinerary(PRIVATE_TOKEN))
                .thenThrow(new ResourceNotFoundException("Public itinerary not found"));

        mockMvc.perform(get("/api/itineraries/public/" + PRIVATE_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Public itinerary not found"));
    }


    @Test
    void getAllItineraries_restituiscePossedutiECondivisi_200() throws Exception {
        when(itineraryService.getAllAccessibleItineraries(OWNER_ID))
                .thenReturn(List.of(privateItinerary(), sharedItinerary()));

        mockMvc.perform(get("/api/itineraries")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(3))
                .andExpect(jsonPath("$[1].visibility").value("SHARED"));

        verify(itineraryService).getAllAccessibleItineraries(OWNER_ID);
    }



    @Test
    void updateVisibility_senzaCampi_400() throws Exception {
        mockMvc.perform(patch("/api/itineraries/1/visibility")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString(
                        "La visibilita' e' obbligatoria")));

        verify(itineraryService, never()).updateVisibility(any(), any(), any());
    }

    @Test
    void createItinerary_visibilityMinuscola_201() throws Exception {
        when(itineraryService.createItinerary(any(), eq(OWNER_ID))).thenReturn(publicItinerary());

        String body = """
                {
                  "title": "Visibilita' minuscola",
                  "visibility": "public",
                  "startDate": "2030-09-01",
                  "endDate": "2030-09-03"
                }
                """;

        mockMvc.perform(post("/api/itineraries")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(itineraryService).createItinerary(any(), eq(OWNER_ID));
    }

    @Test
    void updateVisibility_conVisibilityBooleana_200() throws Exception {
        when(itineraryService.updateVisibility(eq(1L), any(), eq(OWNER_ID))).thenReturn(publicItinerary());

        mockMvc.perform(patch("/api/itineraries/1/visibility")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":true}"))
                .andExpect(status().isOk());

        verify(itineraryService).updateVisibility(eq(1L), any(), eq(OWNER_ID));
    }

    @Test
    void addStopToItinerary_owner_201() throws Exception {
        String stopBody = """
                {
                  "stopOrder": 1,
                  "stopType": "ACTIVITY",
                  "attivitaId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                  "customTitle": "Visita Colosseo",
                  "customDescription": "Tour guidato",
                  "startDatetime": "2030-09-01T10:00:00",
                  "endDatetime": "2030-09-01T12:00:00"
                }
                """;

        mockMvc.perform(post("/api/itineraries/1/stops")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stopBody))
                .andExpect(status().isCreated());

        verify(itineraryService).addStopToItinerary(eq(1L), any(), eq(OWNER_ID));
    }

    @Test
    void addStopToItinerary_sharedUserWithEditAccess_201() throws Exception {
        String stopBody = """
                {
                  "stopOrder": 1,
                  "stopType": "ACTIVITY",
                  "attivitaId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                  "customTitle": "Visita Colosseo",
                  "customDescription": "Tour guidato",
                  "startDatetime": "2030-09-01T10:00:00",
                  "endDatetime": "2030-09-01T12:00:00"
                }
                """;

        mockMvc.perform(post("/api/itineraries/3/stops")
                        .with(jwt().jwt(j -> j.subject(SHARED_USER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stopBody))
                .andExpect(status().isCreated());

        verify(itineraryService).addStopToItinerary(eq(3L), any(), eq(SHARED_USER_ID));
    }

    @Test
    void addStopToItinerary_sharedUserWithoutEditAccess_403() throws Exception {
        String stopBody = """
                {
                  "stopOrder": 1,
                  "stopType": "ACTIVITY",
                  "attivitaId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                  "customTitle": "Visita Colosseo",
                  "customDescription": "Tour guidato",
                  "startDatetime": "2030-09-01T10:00:00",
                  "endDatetime": "2030-09-01T12:00:00"
                }
                """;

        org.mockito.Mockito.doThrow(new AccessDeniedException("Not authorized to modify this itinerary"))
                .when(itineraryService).addStopToItinerary(eq(3L), any(), eq(SHARED_USER_ID));

        mockMvc.perform(post("/api/itineraries/3/stops")
                        .with(jwt().jwt(j -> j.subject(SHARED_USER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stopBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Not authorized to modify this itinerary"));
    }

    @Test
    void addStopToItinerary_stranger_403() throws Exception {
        String stopBody = """
                {
                  "stopOrder": 1,
                  "stopType": "ACTIVITY",
                  "attivitaId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                  "customTitle": "Visita Colosseo",
                  "customDescription": "Tour guidato",
                  "startDatetime": "2030-09-01T10:00:00",
                  "endDatetime": "2030-09-01T12:00:00"
                }
                """;

        org.mockito.Mockito.doThrow(new AccessDeniedException("Not authorized to modify this itinerary"))
                .when(itineraryService).addStopToItinerary(eq(1L), any(), eq(STRANGER_ID));

        mockMvc.perform(post("/api/itineraries/1/stops")
                        .with(jwt().jwt(j -> j.subject(STRANGER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stopBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateItinerary_owner_200() throws Exception {
        String updateBody = """
                {
                  "title": "Titolo Aggiornato",
                  "description": "Nuova descrizione",
                  "startDate": "2030-09-01",
                  "endDate": "2030-09-03"
                }
                """;

        when(itineraryService.updateItinerary(eq(1L), any(), eq(OWNER_ID))).thenReturn(privateItinerary());

        mockMvc.perform(put("/api/itineraries/1")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Vacanza a Roma"));

        verify(itineraryService).updateItinerary(eq(1L), any(), eq(OWNER_ID));
    }

    @Test
    void updateItinerary_sharedUserWithEditAccess_200() throws Exception {
        String updateBody = """
                {
                  "title": "Titolo Aggiornato",
                  "description": "Nuova descrizione",
                  "startDate": "2030-09-01",
                  "endDate": "2030-09-03"
                }
                """;

        when(itineraryService.updateItinerary(eq(3L), any(), eq(SHARED_USER_ID))).thenReturn(sharedItinerary());

        mockMvc.perform(put("/api/itineraries/3")
                        .with(jwt().jwt(j -> j.subject(SHARED_USER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk());

        verify(itineraryService).updateItinerary(eq(3L), any(), eq(SHARED_USER_ID));
    }

    @Test
    void updateItinerary_sharedUserWithoutEditAccess_403() throws Exception {
        String updateBody = """
                {
                  "title": "Titolo Aggiornato",
                  "description": "Nuova descrizione",
                  "startDate": "2030-09-01",
                  "endDate": "2030-09-03"
                }
                """;

        org.mockito.Mockito.doThrow(new AccessDeniedException("Not authorized to modify this itinerary"))
                .when(itineraryService).updateItinerary(eq(3L), any(), eq(SHARED_USER_ID));

        mockMvc.perform(put("/api/itineraries/3")
                        .with(jwt().jwt(j -> j.subject(SHARED_USER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Not authorized to modify this itinerary"));
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
    void deleteItinerary_sharedUser_403() throws Exception {
        org.mockito.Mockito.doThrow(new AccessDeniedException("Only the owner can perform this action"))
                .when(itineraryService).deleteItinerary(eq(3L), eq(SHARED_USER_ID));

        mockMvc.perform(delete("/api/itineraries/3")
                        .with(jwt().jwt(j -> j.subject(SHARED_USER_SUB))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the owner can perform this action"));
    }

    @Test
    void deleteItinerary_stranger_403() throws Exception {
        org.mockito.Mockito.doThrow(new AccessDeniedException("Only the owner can perform this action"))
                .when(itineraryService).deleteItinerary(eq(1L), eq(STRANGER_ID));

        mockMvc.perform(delete("/api/itineraries/1")
                        .with(jwt().jwt(j -> j.subject(STRANGER_SUB))))
                .andExpect(status().isForbidden());
    }

    @Test
    void endpointSenzaToken_401() throws Exception {
        mockMvc.perform(get("/api/itineraries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerSenzaToken_401() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflight_daGateway_rispondeConAccessControlAllowOrigin() throws Exception {
        mockMvc.perform(options("/api/itineraries")
                        .header(HttpHeaders.ORIGIN, "http://localhost:8080")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8080"));
    }

    @Test
    void richiestaConOrigin_daGateway_esponeAccessControlAllowOrigin() throws Exception {
        when(itineraryService.getAllAccessibleItineraries(any(UUID.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/itineraries")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .header(HttpHeaders.ORIGIN, "http://localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8080"));
    }

    @Test
    void corsPreflight_originNonAutorizzato_403() throws Exception {
        mockMvc.perform(options("/api/itineraries")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void corsPreflight_allowCredentialsFalse_nonEsponeHeaderCredenziali() throws Exception {
        mockMvc.perform(options("/api/itineraries")
                        .header(HttpHeaders.ORIGIN, "http://localhost:8080")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
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
        mockMvc.perform(get("/api/non-esistente")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Risorsa non trovata: api/non-esistente"));
    }

    @Test
    void erroreInterno_500ConMessaggioGenerico() throws Exception {
        when(itineraryService.getAllAccessibleItineraries(any(UUID.class)))
                .thenThrow(new RuntimeException("dettaglio interno non esposto"));

        mockMvc.perform(get("/api/itineraries")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Errore interno del server"));
    }

    @Test
    void shareItinerary_utenteValido_201() throws Exception {
        ItineraryShareResponse response = new ItineraryShareResponse(10L, 1L, SHARED_USER_ID, true, null);
        when(itineraryService.shareItinerary(eq(1L), any(), eq(OWNER_ID))).thenReturn(response);

        mockMvc.perform(post("/api/itineraries/1/shares")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sharedWithUserId\":\"" + SHARED_USER_ID + "\",\"canEdit\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sharedWithUserId").value(SHARED_USER_SUB))
                .andExpect(jsonPath("$.canEdit").value(true));

        verify(itineraryService).shareItinerary(eq(1L), any(), eq(OWNER_ID));
    }

    @Test
    void shareItinerary_utenteInesistente_404() throws Exception {
        when(itineraryService.shareItinerary(eq(1L), any(), eq(OWNER_ID)))
                .thenThrow(new ResourceNotFoundException("Utente non trovato: " + SHARED_USER_ID));

        mockMvc.perform(post("/api/itineraries/1/shares")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sharedWithUserId\":\"" + SHARED_USER_ID + "\",\"canEdit\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Utente non trovato: " + SHARED_USER_ID));
    }

    @Test
    void shareItinerary_servizioNonDisponibile_503() throws Exception {
        when(itineraryService.shareItinerary(eq(1L), any(), eq(OWNER_ID)))
                .thenThrow(new UserAuthServiceUnavailableException("user-auth-service non raggiungibile", null));

        mockMvc.perform(post("/api/itineraries/1/shares")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sharedWithUserId\":\"" + SHARED_USER_ID + "\",\"canEdit\":true}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shareItinerary_condivisioneDuplicata_400() throws Exception {
        when(itineraryService.shareItinerary(eq(1L), any(), eq(OWNER_ID)))
                .thenThrow(new IllegalArgumentException("Itinerary already shared with this user"));

        mockMvc.perform(post("/api/itineraries/1/shares")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sharedWithUserId\":\"" + SHARED_USER_ID + "\",\"canEdit\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Itinerary already shared with this user"));
    }

    @Test
    void createItinerary_violazioneVincoloDatabase_409() throws Exception {
        when(itineraryService.createItinerary(any(), eq(OWNER_ID)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        mockMvc.perform(post("/api/itineraries")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Conflitto con i dati esistenti"));
    }

    @Test
    void createItinerary_fineAntecedenteAllInizio_400ConMessaggioChiaro() throws Exception {
        mockMvc.perform(post("/api/itineraries")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Date invertite\",\"startDate\":\"2030-09-10\",\"endDate\":\"2030-09-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("endDate : La data di fine non puo' essere precedente alla data di inizio"));

        verify(itineraryService, never()).createItinerary(any(), any());
    }

    @Test
    void createItinerary_dataInizioNelPassato_400ConMessaggioChiaro() throws Exception {
        mockMvc.perform(post("/api/itineraries")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nel passato\",\"startDate\":\"2020-01-01\",\"endDate\":\"2030-01-03\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startDate : La data di inizio dell'itinerario non puo' essere nel passato"));
    }

    @Test
    void addStop_stopTypeNonValido_400ConMessaggioDescrittivo() throws Exception {
        when(itineraryService.addStopToItinerary(eq(1L), any(), eq(OWNER_ID)))
                .thenThrow(new IllegalArgumentException("stopType non valido: custom. Valori ammessi: TRIP, ACTIVITY, CUSTOM (maiuscolo)"));

        mockMvc.perform(post("/api/itineraries/1/stops")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stopOrder": 1,
                                  "stopType": "custom",
                                  "startDatetime": "2030-09-01T10:00:00",
                                  "endDatetime": "2030-09-01T12:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("stopType non valido: custom. Valori ammessi: TRIP, ACTIVITY, CUSTOM (maiuscolo)"));
    }

    @Test
    void addStop_vincoloCheckDatabase_400InveceDi409() throws Exception {
        when(itineraryService.addStopToItinerary(eq(1L), any(), eq(OWNER_ID)))
                .thenThrow(new DataIntegrityViolationException(
                        "new row for relation \"itinerary_stops\" violates check constraint \"chk_stop_reference\""));

        mockMvc.perform(post("/api/itineraries/1/stops")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stopOrder": 1,
                                  "stopType": "TRIP",
                                  "startDatetime": "2030-09-01T10:00:00",
                                  "endDatetime": "2030-09-01T12:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Riferimenti della tappa incoerenti")));
    }

    @Test
    void illegalArgumentSenzaMessaggio_400ConMessaggioNonVuoto() throws Exception {
        when(itineraryService.getItineraryById(1L, OWNER_ID)).thenThrow(new IllegalArgumentException());

        mockMvc.perform(get("/api/itineraries/1").with(jwt().jwt(j -> j.subject(OWNER_SUB))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Richiesta non valida"));
    }

    @Test
    void createItinerary_restituisceCreatedAtInUtcConOffset() throws Exception {
        ItineraryResponse withTimestamps = new ItineraryResponse(
                1L, OWNER_ID, "Vacanza a Roma", null, Visibility.PRIVATE, null, null,
                java.time.OffsetDateTime.of(2026, 10, 27, 11, 15, 0, 0, java.time.ZoneOffset.UTC),
                java.time.OffsetDateTime.of(2026, 10, 27, 11, 15, 0, 0, java.time.ZoneOffset.UTC),
                List.of(), null, true);
        when(itineraryService.createItinerary(any(), eq(OWNER_ID))).thenReturn(withTimestamps);

        mockMvc.perform(post("/api/itineraries")
                        .with(jwt().jwt(j -> j.subject(OWNER_SUB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").value("2026-10-27T11:15:00Z"))
                .andExpect(jsonPath("$.canEdit").value(true));
    }

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
