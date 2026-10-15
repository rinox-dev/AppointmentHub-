package com.appointmenthub.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointmenthub.support.ApiClient;
import com.appointmenthub.support.ApiTestBase;
import com.appointmenthub.support.TestData;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for error responses (NFR-08, AC-11).
 *
 * <p>Every error must use a consistent structure with a functional error code, a
 * clear message and a matching HTTP status, and must never expose internal
 * implementation details such as stack traces or class names.</p>
 */
@Tag("api")
class ErrorContractApiTest extends ApiTestBase {

    private List<ApiClient.Response> sampleErrors() {
        LocalDate date = TestData.freeDate();
        String startTime = firstAvailableSlot(TestData.ACTIVE_SERVICE_ID, date);
        api.post("/api/appointments", TestData.validBookingBody(date, startTime));

        return List.of(
                api.get("/api/appointments/" + TestData.UNKNOWN_APPOINTMENT_ID),
                api.delete("/api/appointments/" + TestData.UNKNOWN_APPOINTMENT_ID),
                api.get("/api/availability?serviceId=99&date=" + date),
                api.get("/api/availability?serviceId=1&date=not-a-date"),
                api.post("/api/appointments", TestData.validBookingBody(date, startTime)),
                api.post("/api/appointments", TestData.validBookingBody(TestData.pastDate(), "10:00")),
                api.postRaw("/api/appointments", "not json at all"),
                api.get("/api/does-not-exist"));
    }

    @Test
    @DisplayName("NFR-08: every error response uses the documented structure")
    void errorResponsesUseTheDocumentedStructure() {
        for (ApiClient.Response response : sampleErrors()) {
            assertAll(
                    () -> assertTrue(response.status() >= 400, "sample must be an error: " + response),
                    () -> assertNotNull(response.errorCode(), "errorCode is present: " + response),
                    () -> assertNotNull(response.message(), "message is present: " + response),
                    () -> assertEquals(response.status(), response.number("status"),
                            "the status field matches the HTTP status code"),
                    () -> assertEquals(3, response.json().entrySet().size(),
                            "the error body contains exactly errorCode, message and status"));
        }
    }

    @Test
    @DisplayName("NFR-08: error messages are understandable and free of internal details")
    void errorMessagesDoNotLeakInternals() {
        for (ApiClient.Response response : sampleErrors()) {
            String message = response.message();
            assertAll(
                    () -> assertFalse(message.isBlank(), "the message is not empty"),
                    () -> assertFalse(message.contains("Exception"), "no exception class names: " + message),
                    () -> assertFalse(message.contains("com.appointmenthub"), "no package names: " + message),
                    () -> assertFalse(message.contains("\tat "), "no stack trace lines: " + message));
        }
    }

    @Test
    @DisplayName("NFR-08: each functional error code maps to one HTTP status")
    void errorCodesMapToDocumentedStatuses() {
        ApiClient.Response notFound = api.get("/api/appointments/" + TestData.UNKNOWN_APPOINTMENT_ID);
        ApiClient.Response conflict = api.post("/api/appointments", TestData.validBookingBody(
                TestData.futureDate(-1), "10:00"));
        ApiClient.Response validation = api.get("/api/availability?serviceId=1");
        ApiClient.Response missingBody = api.postRaw("/api/appointments", "");

        assertAll(
                () -> assertEquals(404, notFound.status()),
                () -> assertEquals("APPOINTMENT_NOT_FOUND", notFound.errorCode()),
                () -> assertEquals(400, conflict.status()),
                () -> assertEquals("PAST_DATE", conflict.errorCode()),
                () -> assertEquals(400, validation.status()),
                () -> assertEquals("VALIDATION_ERROR", validation.errorCode()),
                () -> assertEquals(400, missingBody.status()),
                () -> assertEquals("MISSING_BODY", missingBody.errorCode()));
    }
}
