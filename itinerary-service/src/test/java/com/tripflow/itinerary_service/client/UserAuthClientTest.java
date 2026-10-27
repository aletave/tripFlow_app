package com.tripflow.itinerary_service.client;

import com.tripflow.itinerary_service.exception.UserAuthServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UserAuthClientTest {

    private static final String BASE_URL = "http://localhost:8086";
    private static final UUID KEYCLOAK_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String LOOKUP_URL = BASE_URL + "/api/auth/users/by-keycloak/" + KEYCLOAK_ID;

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final UserAuthClient client = new UserAuthClient(
            builder.baseUrl(BASE_URL)
                    .requestInterceptor(new BearerTokenForwardingInterceptor())
                    .build());

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void userExists_utentePresente_restituisceTrue() {
        server.expect(requestTo(LOOKUP_URL))
                .andRespond(withSuccess("{\"id\":\"" + KEYCLOAK_ID + "\"}", MediaType.APPLICATION_JSON));

        assertThat(client.userExists(KEYCLOAK_ID)).isTrue();
        server.verify();
    }

    @Test
    void userExists_utenteAssente_restituisceFalse() {
        server.expect(requestTo(LOOKUP_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.userExists(KEYCLOAK_ID)).isFalse();
        server.verify();
    }

    @Test
    void userExists_erroreServer_lanciaEccezione() {
        server.expect(requestTo(LOOKUP_URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.userExists(KEYCLOAK_ID))
                .isInstanceOf(UserAuthServiceUnavailableException.class);
        server.verify();
    }

    @Test
    void userExists_propagaBearerTokenDellaRichiesta() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        server.expect(requestTo(LOOKUP_URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(client.userExists(KEYCLOAK_ID)).isTrue();
        server.verify();
    }
}
