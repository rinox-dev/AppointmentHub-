package com.appointmenthub.api;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Serves the web interface from {@code src/main/resources/static} (PDR section 5.1).
 *
 * <p>The interface is a small single-page application that talks to the REST API
 * through {@code fetch}. Serving it from the same origin keeps the setup
 * beginner friendly: one command starts the whole system.</p>
 */
public final class StaticFileHandler {

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            ".html", "text/html; charset=utf-8",
            ".css", "text/css; charset=utf-8",
            ".js", "application/javascript; charset=utf-8",
            ".json", "application/json; charset=utf-8",
            ".svg", "image/svg+xml",
            ".ico", "image/x-icon");

    public void handle(HttpExchange exchange) {
        try {
            if (!"GET".equals(exchange.getRequestMethod()) && !"HEAD".equals(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (path.contains("..")) {
                sendText(exchange, 400, "Invalid path");
                return;
            }
            String resource = (path == null || path.isEmpty() || "/".equals(path))
                    ? "/static/index.html"
                    : "/static" + path;
            try (InputStream in = StaticFileHandler.class.getResourceAsStream(resource)) {
                if (in == null) {
                    sendText(exchange, 404, "Resource not found: " + path);
                    return;
                }
                byte[] body = in.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", contentTypeFor(resource));
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            }
        } catch (IOException e) {
            System.err.println("Unable to serve the web interface: " + e.getMessage());
        } finally {
            exchange.close();
        }
    }

    private String contentTypeFor(String resource) {
        int dot = resource.lastIndexOf('.');
        if (dot < 0) {
            return "application/octet-stream";
        }
        return CONTENT_TYPES.getOrDefault(resource.substring(dot).toLowerCase(), "application/octet-stream");
    }

    private void sendText(HttpExchange exchange, int status, String message) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
