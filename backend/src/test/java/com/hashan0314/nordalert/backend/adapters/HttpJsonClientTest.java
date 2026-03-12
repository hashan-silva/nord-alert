package com.hashan0314.nordalert.backend.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpJsonClientTest {

  private HttpServer httpServer;
  private HttpJsonClient httpJsonClient;

  @BeforeEach
  void setUp() throws IOException {
    httpServer = HttpServer.create(new InetSocketAddress(0), 0);
    httpJsonClient = new HttpJsonClient(new ObjectMapper());
  }

  @AfterEach
  void tearDown() {
    httpServer.stop(0);
  }

  @Test
  void shouldReturnParsedJsonForSuccessfulResponse() {
    httpServer.createContext("/ok", exchange -> respond(exchange, 200, "{\"status\":\"ok\"}"));
    httpServer.start();

    String url = "http://127.0.0.1:" + httpServer.getAddress().getPort() + "/ok";

    assertEquals("ok", httpJsonClient.getJson(url).path("status").asText());
  }

  @Test
  void shouldThrowForNonSuccessfulResponse() {
    httpServer.createContext("/error", exchange -> respond(exchange, 500, "{\"error\":\"boom\"}"));
    httpServer.start();

    String url = "http://127.0.0.1:" + httpServer.getAddress().getPort() + "/error";

    assertThrows(IllegalStateException.class, () -> httpJsonClient.getJson(url));
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream outputStream = exchange.getResponseBody()) {
      outputStream.write(bytes);
    }
  }
}
