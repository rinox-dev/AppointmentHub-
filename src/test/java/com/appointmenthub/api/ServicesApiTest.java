package com.appointmenthub.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointmenthub.support.ApiClient;
import com.appointmenthub.support.ApiTestBase;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * API tests for {@code GET /api/services} (FR-01, TC-001).
 */
@Tag("api")
class ServicesApiTest extends ApiTestBase {

    @Test
    @DisplayName("TC-001: active services are returned with the documented structure")
    void activeServicesAreReturned() {
        ApiClient.Response response = api.get("/api/services");

        assertAll(
                () -> assertEquals(200, response.status()),
                () -> assertEquals(4, response.jsonArray().size(), "only active services are returned"),
                () -> assertTrue(response.jsonArray().asList().stream()
                        .map(element -> element.getAsJsonObject().get("active").getAsBoolean())
                        .allMatch(Boolean::booleanValue)));
    }

    @Test
    @DisplayName("TC-001: every service exposes the documented fields and types")
    void serviceFieldsMatchTheContract() {
        JsonArray services = api.get("/api/services").jsonArray();

        for (int index = 0; index < services.size(); index++) {
            JsonObject service = services.get(index).getAsJsonObject();
            assertAll(
                    () -> assertTrue(service.get("id").isJsonPrimitive() && service.get("id").getAsJsonPrimitive().isNumber(),
                            "id is a number"),
                    () -> assertTrue(service.get("name").isJsonPrimitive() && service.get("name").getAsJsonPrimitive().isString(),
                            "name is a string"),
                    () -> assertTrue(service.get("durationMinutes").getAsInt() > 0, "duration is positive"),
                    () -> assertTrue(service.get("active").getAsJsonPrimitive().isBoolean(), "active is a boolean"),
                    () -> assertEquals(4, service.entrySet().size(), "no undocumented extra fields"));
        }
    }

    @Test
    @DisplayName("TC-001 (extension): inactive services are visible with includeInactive=true")
    void inactiveServicesAreAvailableForTesting() {
        ApiClient.Response response = api.get("/api/services?includeInactive=true");

        assertAll(
                () -> assertEquals(200, response.status()),
                () -> assertEquals(5, response.jsonArray().size()),
                () -> assertFalse(response.jsonArray().asList().stream()
                        .map(element -> element.getAsJsonObject().get("active").getAsBoolean())
                        .allMatch(Boolean::booleanValue), "the catalog contains one inactive service"));
    }

    @Test
    @DisplayName("NFR-08: unsupported methods and unknown paths return controlled errors")
    void unsupportedRequestsReturnControlledErrors() {
        ApiClient.Response wrongMethod = api.post("/api/services", "{}");
        ApiClient.Response unknownPath = api.get("/api/services/1");

        assertAll(
                () -> assertEquals(405, wrongMethod.status()),
                () -> assertEquals("METHOD_NOT_ALLOWED", wrongMethod.errorCode()),
                () -> assertEquals(404, unknownPath.status()),
                () -> assertEquals("NOT_FOUND", unknownPath.errorCode()));
    }
}
