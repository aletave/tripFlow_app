package com.tripflow.user_auth_service.config.handler;

import com.tripflow.user_auth_service.dto.errors.ServiceError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @Test
    void onRisorsaNonTrovata_404ConPathDellaRisorsa() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        WebRequest req = mock(WebRequest.class);
        when(req.resolveReference("request")).thenReturn(null);

        ServiceError errore = handler.onRisorsaNonTrovata(req,
                new NoResourceFoundException(HttpMethod.GET, "/api/auth/inesistente"));

        assertNotNull(errore.getTimestamp());
        assertEquals("/api/auth/inesistente", errore.getMessage().replace("Risorsa non trovata: ", ""));
        assertEquals("sconosciuto", errore.getPath());
    }
}