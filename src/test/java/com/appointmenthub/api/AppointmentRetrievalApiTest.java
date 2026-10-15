package com.appointmenthub.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointmenthub.support.ApiClient;
import com.appointmenthub.support.ApiTestBase;
import com.appointmenthub.support.TestData;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * API tests for {@code GET /api/appointments} and {@code GET /api/appointments/{id}}
 * (FR-05, FR-06, TC-009, TC-010).
 */
@Tag("api")
class AppointmentRetrievalApiTest extends ApiTestBase {

    private ApiClient.Response book(LocalDate date) {
        String startTime = firstAvailableSlot(TestData.ACTIVE_SERVICE_ID, date);
        return api.post("/api/appointments", TestData.validBookingBody(date, startTime));
    }

    @Test
    @DisplayName("TC-009: an existing appointment is returned with its full details")
    void existingAppointmentIsReturned() {
        LocalDate date = TestData.freeDate();
        ApiClient.Response created = book(date);

        ApiClient.Response retrieved = api.get("/api/appointments/" + created.number("id"));

        assertAll(
                () -> assertEquals(200, retrieved.status()),
                () -> assertEquals(created.number("id"), retrieved.number("id")),
                () -> assertEquals(TestData.VALID_NAME, retrieved.string("customerName")),
                () -> assertEquals(TestData.VALID_EMAIL, retrieved.string("customerEmail")),
                () -> assertEquals(TestData.ACTIVE_SERVICE_ID, retrieved.number("serviceId")),
                () -> assertEquals(date.toString(), retrieved.string("appointmentDate")),
                () -> assertEquals(created.string("startTime"), retrieved.string("startTime")),
                () -> assertEquals(created.string("endTime"), retrieved.string("endTime")),
                () -> assertEquals("CONFIRMED", retrieved.string("status")));
    }

    @Test
    @DisplayName("TC-010: a nonexistent appointment returns 404 with a functional error code")
    void nonexistentAppointmentReturnsNotFound() {
        ApiClient.Response response = api.get("/api/appointments/" + TestData.UNKNOWN_APPOINTMENT_ID);

        assertAll(
                () -> assertEquals(404, response.status()),
                () -> assertEquals("APPOINTMENT_NOT_FOUND", response.errorCode()),
                () -> assertTrue(response.message().contains(String.valueOf(TestData.UNKNOWN_APPOINTMENT_ID))));
    }

    @Test
    @DisplayName("TC-010: a non numeric identifier is rejected as invalid input")
    void nonNumericIdentifierIsRejected() {
        ApiClient.Response response = api.get("/api/appointments/not-a-number");

        assertAll(
                () -> assertEquals(400, response.status()),
                () -> assertEquals("VALIDATION_ERROR", response.errorCode()));
    }

    @Test
    @DisplayName("FR-06: registered appointments include the newly created one")
    void registeredAppointmentsIncludeTheCreatedOne() {
        LocalDate date = TestData.freeDate();
        ApiClient.Response created = book(date);

        ApiClient.Response list = api.get("/api/appointments");

        assertAll(
                () -> assertEquals(200, list.status()),
                () -> assertTrue(list.jsonArray().size() >= 1),
                () -> assertTrue(list.jsonArray().asList().stream()
                                .anyMatch(element -> element.getAsJsonObject().get("id").getAsInt() == created.number("id")),
                        "the created appointment is part of the list"),
                () -> assertEquals(created.number("id"),
                        list.jsonArray().get(0).getAsJsonObject().get("id").getAsInt(),
                        "the most recent appointment comes first"));
    }

    @Test
    @DisplayName("FR-06: the appointment list can be filtered by customer email")
    void appointmentListCanBeFilteredByEmail() {
        LocalDate date = TestData.freeDate();
        book(date);
        String secondSlot = api.get("/api/availability?serviceId=1&date=" + date)
                .json().getAsJsonArray("slots").get(1).getAsJsonObject().get("startTime").getAsString();
        api.post("/api/appointments", TestData.bookingBody(TestData.SECOND_NAME, TestData.SECOND_EMAIL,
                TestData.ACTIVE_SERVICE_ID, date, secondSlot));

        ApiClient.Response anaOnly = api.get("/api/appointments?customerEmail=" + TestData.VALID_EMAIL);
        ApiClient.Response nobody = api.get("/api/appointments?customerEmail=nobody@example.com");

        assertAll(
                () -> assertTrue(anaOnly.jsonArray().size() >= 1),
                () -> assertTrue(anaOnly.jsonArray().asList().stream().allMatch(element ->
                        TestData.VALID_EMAIL.equals(element.getAsJsonObject().get("customerEmail").getAsString()))),
                () -> assertEquals(0, nobody.jsonArray().size()));
    }
}
