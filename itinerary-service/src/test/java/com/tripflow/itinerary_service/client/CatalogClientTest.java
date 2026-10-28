package com.tripflow.itinerary_service.client;

import com.tripflow.itinerary_service.exception.CatalogServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatalogClientTest {

    private static final String BASE_URL = "http://localhost:8084";
    private static final UUID ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final CatalogClient client = new CatalogClient(builder.baseUrl(BASE_URL).build());

    @Test
    void tripExists_viaggioPresente_true() {
        server.expect(requestTo(BASE_URL + "/api/v1/trips/" + ID))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(client.tripExists(ID)).isTrue();
        server.verify();
    }

    @Test
    void tripExists_viaggioAssente_false() {
        server.expect(requestTo(BASE_URL + "/api/v1/trips/" + ID))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.tripExists(ID)).isFalse();
        server.verify();
    }

    @Test
    void activityExists_usaEndpointAttivita() {
        server.expect(requestTo(BASE_URL + "/api/v1/activities/" + ID))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.activityExists(ID)).isFalse();
        server.verify();
    }

    @Test
    void catalogoInErrore_lanciaCatalogServiceUnavailable() {
        server.expect(requestTo(BASE_URL + "/api/v1/trips/" + ID))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.tripExists(ID))
                .isInstanceOf(CatalogServiceUnavailableException.class);
        server.verify();
    }
}
