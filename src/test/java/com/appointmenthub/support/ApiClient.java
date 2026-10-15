package com.appointmenthub.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Small HTTP client used by the API tests.
 *
 * <p>It returns the status code, the raw body and the elapsed time, so a test can
 * assert on the contract (status, structure, values) and on response time
 * (NFR-02) without extra tooling.</p>
 */
public final class ApiClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public Response get(String path) {
        return send(request(path).GET().build());
    }

    public Response post(String path, String jsonBody) {
        return send(request(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build());
    }

    /** Posts a body without declaring a JSON content type; used by negative tests. */
    public Response postRaw(String path, String body) {
        return send(request(path).POST(HttpRequest.BodyPublishers.ofString(body)).build());
    }

    public Response delete(String path) {
        return send(request(path).DELETE().build());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(TIMEOUT);
    }

    private Response send(HttpRequest request) {
        long startedAt = System.nanoTime();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            return new Response(response.statusCode(), response.body(), elapsedMillis);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP request to " + request.uri() + " failed", e);
        }
    }

    /** HTTP response with helpers to read JSON fields. */
    public record Response(int status, String body, long elapsedMillis) {

        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }

        public JsonObject json() {
            return JsonParser.parseString(body).getAsJsonObject();
        }

        public JsonArray jsonArray() {
            return JsonParser.parseString(body).getAsJsonArray();
        }

        public String string(String field) {
            JsonElement value = json().get(field);
            return value == null || value.isJsonNull() ? null : value.getAsString();
        }

        public int number(String field) {
            return json().get(field).getAsInt();
        }

        /** Functional error code, or {@code null} when the response is not an error. */
        public String errorCode() {
            return json().has("errorCode") ? string("errorCode") : null;
        }

        public String message() {
            return json().has("message") ? string("message") : null;
        }

        @Override
        public String toString() {
            return "HTTP " + status + " " + body;
        }
    }
}
