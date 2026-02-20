package io.github.anandb.mockserver.integration;

import io.github.anandb.mockserver.model.RelayConfig;
import io.github.anandb.mockserver.service.OAuth2TokenService;
import io.github.anandb.mockserver.service.RelayService;
import io.github.anandb.mockserver.util.HttpClientFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RelaySslIntegrationTest {

    private static ClientAndServer targetServer;
    private RelayService relayService;
    private HttpClientFactory httpClientFactory;

    @BeforeAll
    static void startServer() {
        targetServer = ClientAndServer.startClientAndServer(0);
        targetServer.when(
                HttpRequest.request()
                        .withPath("/test")
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withBody("Success")
        );
    }

    @AfterAll
    static void stopServer() {
        if (targetServer != null) {
            targetServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        httpClientFactory = new HttpClientFactory();
        OAuth2TokenService tokenService = mock(OAuth2TokenService.class);
        relayService = new RelayService(tokenService, httpClientFactory);
    }

    @Test
    void testRelayToHttpsServerWithSslErrorsIgnored() throws Exception {
        int port = targetServer.getLocalPort();
        // Use https URL. MockServer supports both http and https on the same port by default.
        String remoteUrl = "https://localhost:" + port;

        RelayConfig config = new RelayConfig();
        config.setRemoteUrl(remoteUrl);
        config.setIgnoreSSLErrors(true);

        RelayService.RelayResponse response = relayService.relayRequest(
                config,
                "GET",
                "/test",
                Collections.emptyMap(),
                null
        );

        assertEquals(200, response.statusCode());
        assertEquals("Success", new String(response.body()));
    }

    @Test
    void testRelayToHttpsServerWithSslErrorsNotIgnored() {
        int port = targetServer.getLocalPort();
        String remoteUrl = "https://localhost:" + port;

        RelayConfig config = new RelayConfig();
        config.setRemoteUrl(remoteUrl);
        config.setIgnoreSSLErrors(false);

        // This should throw an exception (SSLHandshakeException) because the certificate is self-signed
        Exception exception = assertThrows(Exception.class, () -> {
            relayService.relayRequest(
                    config,
                    "GET",
                    "/test",
                    Collections.emptyMap(),
                    null
            );
        });

        // The cause should be an SSL related exception
        assertTrue(exception.getMessage().contains("SSL") ||
                   (exception.getCause() != null && exception.getCause().getMessage().contains("SSL")) ||
                   exception.getMessage().contains("PKIX path building failed"));
    }
}
