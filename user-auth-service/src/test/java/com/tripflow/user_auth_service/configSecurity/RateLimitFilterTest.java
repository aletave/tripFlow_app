package com.tripflow.user_auth_service.configSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(true, 2, 60);
    }

    private MockHttpServletRequest richiestaPostRegister(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        request.setRemoteAddr(ip);
        return request;
    }

    private MockHttpServletResponse esegui(RateLimitFilter filtro, MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void lePrimeRichiestePassano_laTerzaVieneBloccata() throws Exception {
        assertEquals(200, esegui(filter, richiestaPostRegister("10.0.0.1")).getStatus());
        assertEquals(200, esegui(filter, richiestaPostRegister("10.0.0.1")).getStatus());

        MockHttpServletResponse bloccata = esegui(filter, richiestaPostRegister("10.0.0.1"));

        assertEquals(429, bloccata.getStatus());
        assertTrue(bloccata.getContentAsString().contains("Troppe richieste"));
        assertTrue(bloccata.getContentAsString().contains("\"path\""));
    }

    @Test
    void finestraScaduta_ilContatoreRiparte() throws Exception {
        RateLimitFilter filtroBreve = new RateLimitFilter(true, 2, 1);

        assertEquals(200, esegui(filtroBreve, richiestaPostRegister("10.0.0.2")).getStatus());
        assertEquals(200, esegui(filtroBreve, richiestaPostRegister("10.0.0.2")).getStatus());
        assertEquals(429, esegui(filtroBreve, richiestaPostRegister("10.0.0.2")).getStatus());

        Thread.sleep(1100);

        assertEquals(200, esegui(filtroBreve, richiestaPostRegister("10.0.0.2")).getStatus());
    }

    @Test
    void gliEndpointNonRegistrazioneNonSonoLimitati() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.setRemoteAddr("10.0.0.3");

        assertEquals(200, esegui(filter, request).getStatus());
    }

    @Test
    void xForwardedFor_ipRealiConteggiatiSeparatamente() throws Exception {
        MockHttpServletRequest richiesta1 = richiestaPostRegister("10.0.0.4");
        richiesta1.addHeader("X-Forwarded-For", "1.2.3.4");
        MockHttpServletRequest richiesta2 = richiestaPostRegister("10.0.0.4");
        richiesta2.addHeader("X-Forwarded-For", "5.6.7.8");

        assertEquals(200, esegui(filter, richiesta1).getStatus());
        assertEquals(200, esegui(filter, richiesta2).getStatus());
        assertEquals(200, esegui(filter, richiesta1).getStatus());
        assertEquals(429, esegui(filter, richiesta1).getStatus());
        assertEquals(200, esegui(filter, richiesta2).getStatus());
        assertEquals(429, esegui(filter, richiesta2).getStatus());
    }

    @Test
    void filtroDisabilitato_nonBloccaMai() throws Exception {
        RateLimitFilter disabilitato = new RateLimitFilter(false, 1, 60);

        for (int i = 0; i < 5; i++) {
            assertEquals(200, esegui(disabilitato, richiestaPostRegister("10.0.0.5")).getStatus());
        }
    }
}