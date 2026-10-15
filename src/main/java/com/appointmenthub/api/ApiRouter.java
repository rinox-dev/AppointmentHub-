package com.appointmenthub.api;

import com.appointmenthub.error.ApiException;
import com.appointmenthub.error.ErrorCode;
import com.appointmenthub.json.Json;
import com.appointmenthub.model.CreateAppointmentRequest;
import com.appointmenthub.service.BookingService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API router implementing the contract documented in {@code docs/api-spec.md}
 * (PDR section 10).
 *
 * <p>Responsibilities: read the request, delegate to {@link BookingService}, and
 * translate the result (or an {@link ApiException}) into a consistent HTTP
 * response. No business rule lives here.</p>
 */
public final class ApiRouter {

    private final BookingService bookingService;
    private final Gson gson = Json.gson();

    public ApiRouter(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public void handle(HttpExchange exchange) {
        try {
            route(exchange);
        } catch (ApiException e) {
            sendError(exchange, e.httpStatus(), e.errorCode().name(), e.getMessage());
        } catch (JsonParseException e) {
            sendError(exchange, ErrorCode.MALFORMED_REQUEST.httpStatus(), ErrorCode.MALFORMED_REQUEST.name(),
                    "The request body is not valid JSON for the expected contract.");
        } catch (Exception e) {
            // Internal details are logged server-side but never exposed (NFR-08).
            e.printStackTrace();
            sendError(exchange, ErrorCode.INTERNAL_ERROR.httpStatus(), ErrorCode.INTERNAL_ERROR.name(),
                    "An unexpected error occurred while processing the request.");
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String path = normalize(exchange.getRequestURI().getPath());
        String method = exchange.getRequestMethod();

        if (path.equals("/api/services")) {
            requireMethod(method, "GET");
            boolean includeInactive = "true".equalsIgnoreCase(queryParams(exchange).get("includeInactive"));
            sendJson(exchange, 200, bookingService.services(includeInactive));
        } else if (path.equals("/api/availability")) {
            requireMethod(method, "GET");
            Map<String, String> params = queryParams(exchange);
            sendJson(exchange, 200, bookingService.availability(params.get("serviceId"), params.get("date")));
        } else if (path.equals("/api/appointments")) {
            if ("GET".equals(method)) {
                sendJson(exchange, 200, bookingService.findAll(queryParams(exchange).get("customerEmail")));
            } else if ("POST".equals(method)) {
                sendJson(exchange, 201, bookingService.create(readBodyAsRequest(exchange)));
            } else {
                throw new ApiException(ErrorCode.METHOD_NOT_ALLOWED,
                        "/api/appointments supports GET and POST, not " + method + ".");
            }
        } else if (path.startsWith("/api/appointments/")) {
            int id = parseId(path.substring("/api/appointments/".length()));
            if ("GET".equals(method)) {
                sendJson(exchange, 200, bookingService.findById(id));
            } else if ("DELETE".equals(method)) {
                sendJson(exchange, 200, bookingService.cancel(id));
            } else {
                throw new ApiException(ErrorCode.METHOD_NOT_ALLOWED,
                        "/api/appointments/{id} supports GET and DELETE, not " + method + ".");
            }
        } else {
            throw new ApiException(ErrorCode.NOT_FOUND, "No API resource matches " + path + ".");
        }
    }

    private CreateAppointmentRequest readBodyAsRequest(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        if (body == null || body.isBlank()) {
            throw new ApiException(ErrorCode.MISSING_BODY, "A JSON request body is required.");
        }
        return gson.fromJson(body, CreateAppointmentRequest.class);
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void requireMethod(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new ApiException(ErrorCode.METHOD_NOT_ALLOWED,
                    "This endpoint supports " + expected + ", not " + actual + ".");
        }
    }

    private int parseId(String rawId) {
        String value = rawId.replace("/", "").trim();
        if (value.isEmpty()) {
            throw ApiException.validation("An appointment identifier is required.");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw ApiException.validation("The appointment identifier must be an integer (received '" + rawId + "').");
        }
    }

    private String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String pair : query.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int separator = pair.indexOf('=');
            String key = separator < 0 ? pair : pair.substring(0, separator);
            String value = separator < 0 ? "" : pair.substring(separator + 1);
            params.put(URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return params;
    }

    private void sendJson(HttpExchange exchange, int status, Object payload) {
        send(exchange, status, gson.toJson(payload));
    }

    private void sendError(HttpExchange exchange, int status, String errorCode, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("errorCode", errorCode);
        error.addProperty("message", message);
        error.addProperty("status", status);
        send(exchange, status, gson.toJson(error));
    }

    private void send(HttpExchange exchange, int status, String json) {
        try {
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        } catch (IOException e) {
            // The client closed the connection; nothing else can be done here.
            System.err.println("Unable to send the HTTP response: " + e.getMessage());
        }
    }
}
