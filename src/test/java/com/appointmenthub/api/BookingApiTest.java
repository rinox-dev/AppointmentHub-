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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * API tests for {@code POST /api/appointments} (FR-03 to FR-10, TC-003 to TC-008,
 * TC-016 to TC-018, TC-021, TC-022).
 */
@Tag("api")
class BookingApiTest extends ApiTestBase {

    private int appointmentCount() {
        return api.get("/api/appointments").jsonArray().size();
    }

    @Test
    @DisplayName("TC-003/AC-03: booking with valid data returns 201 and a unique identifier")
    void bookingWithValidDataCreatesAppointment() {
        LocalDate date = TestData.freeDate();
        String startTime = firstAvailableSlot(TestData.ACTIVE_SERVICE_ID, date);

        ApiClient.Response response = api.post("/api/appointments", TestData.validBookingBody(date, startTime));

        assertAll(
                () -> assertEquals(201, response.status()),
                () -> assertTrue(response.number("id") > 0, "an identifier is returned"),
                () -> assertEquals("CONFIRMED", response.string("status")),
                () -> assertEquals(TestData.VALID_NAME, response.string("customerName")),
                () -> assertEquals(TestData.VALID_EMAIL, response.string("customerEmail")),
                () -> assertEquals(date.toString(), response.string("appointmentDate")),
                () -> assertEquals(startTime, response.string("startTime")),
                () -> assertNotNull(response.string("createdAt")),
                () -> assertEquals(9, response.json().entrySet().size(), "the documented fields are returned"));
    }

    @Test
    @DisplayName("TC-003/TC-009: a created appointment can be retrieved through its identifier")
    void createdAppointmentCanBeRetrieved() {
        LocalDate date = TestData.freeDate();
        String startTime = firstAvailableSlot(TestData.ACTIVE_SERVICE_ID, date);
        ApiClient.Response created = api.post("/api/appointments", TestData.validBookingBody(date, startTime));

        ApiClient.Response retrieved = api.get("/api/appointments/" + created.number("id"));

        assertAll(
                () -> assertEquals(200, retrieved.status()),
                () -> assertEquals(created.json(), retrieved.json(), "the retrieved appointment matches the created one"));
    }

    @Test
    @DisplayName("FR-04/BR-11: identifiers are unique across appointments")
    void identifiersAreUnique() {
        LocalDate date = TestData.freeDate();
        ApiClient.Response first = api.post("/api/appointments",
                TestData.validBookingBody(date, firstAvailableSlot(TestData.ACTIVE_SERVICE_ID, date)));

        String secondStart = api.get("/api/availability?serviceId=1&date=" + date)
                .json().getAsJsonArray("slots").get(1).getAsJsonObject().get("startTime").getAsString();
        ApiClient.Response second = api.post("/api/appointments", TestData.validBookingBody(date, secondStart));

        assertAll(
                () -> assertEquals(201, first.status()),
                () -> assertEquals(201, second.status()),
                () -> assertFalse(first.number("id") == second.number("id")));
    }

    @Test
    @DisplayName("TC-004/FR-09: booking without a name is rejected")
    void bookingWithoutNameIsRejected() {
        LocalDate date = TestData.freeDate();
        String startTime = firstAvailableSlot(TestData.ACTIVE_SERVICE_ID, date);
        int before = appointmentCount();

        ApiClient.Response response = api.post("/api/appointments",
                TestData.bookingBodyWithout("customerName", date, startTime));

        assertAll(
                () -> assertEquals(400, response.status()),
                () -> assertEquals("VALIDATION_ERROR", response.errorCode()),
                () -> assertTrue(response.message().contains("customerName")),
                () -> assertEquals(before, appointmentCount(), "no appointment is created"));
    }

    @Test
    @DisplayName("TC-005/FR-09: booking without an email is rejected")
    void bookingWithoutEmailIsRejected() {
        LocalDate date = TestData.freeDate();
        String startTime = firstAvailableSlot(TestData.ACTIVE_SERVICE_ID, date);
        int before = appointmentCount();

        ApiClient.Response response = api.post("/api/appointments",
                TestData.bookingBodyWithout("customerEmail", date, startTime));

        assertAll(
                () -> assertEquals(400, response.status()),
                () -> assertEquals("VALIDATION_ERROR", response.errorCode()),
                () -> assertTrue(response.message().contains("customerEmail")),
                () -> assertEquals(before, appointmentCount()));
    }

    @Test
    @DisplayName("TC-006/BR-02: booking with a malformed email is rejected")
    void bookingWithMalformedEmailIsRejected() {
        LocalDate date = TestData.freeDate();
        String startTime = firstAvailableSlot(TestData.ACTIVE_SERVICE_ID, date);

        ApiClient.Response response = api.post("/api/appointments",
                TestData.bookingBody(TestData.VALID_NAME, "ana.example.com", TestData.ACTIVE_SERVICE_ID, date, startTime));

        assertAll(
                () -> assertEquals(400, response.status()),
                () -> assertEquals("VALIDATION_ERROR", response.errorCode()));
    }

    @Test
    @DisplayName("TC-007/FR-10: booking for a past date is rejected")
    void bookingForPastDateIsRejected() {
        LocalDate pastDate = TestData.pastDate();

        ApiClient.Response response = api.post("/api/appointments",
                TestData.validBookingBody(pastDate, "10:00"));

        assertAll(
                () -> assertEquals(400, response.status()),
                () -> assertEquals("PAST_DATE", response.errorCode()),
                () -> assertTrue(response.message().contains(pastDate.toString())));
    }

    @Test
    @DisplayName("TC-008/FR-08: booking an occupied slot is rejected and creates no duplicate")
    void bookingOccupiedSlotIsRejected() {
        LocalDate date = TestData.freeDate();
        String startTime = firstAvailableSlot(TestData.ACTIVE_SERVICE_ID, date);
        ApiClient.Response first = api.post("/api/appointments", TestData.validBookingBody(date, startTime));
        int afterFirst = appointmentCount();

        ApiClient.Response second = api.post("/api/appointments",
                TestData.bookingBody(TestData.SECOND_NAME, TestData.SECOND_EMAIL, TestData.SECOND_SERVICE_ID, date, startTime));

        assertAll(
                () -> assertEquals(201, first.status()),
                () -> assertEquals(409, second.status()),
                () -> assertEquals("SLOT_NOT_AVAILABLE", second.errorCode()),
                () -> assertEquals(afterFirst, appointmentCount(), "the second request creates nothing"),
                () -> assertEquals("CONFIRMED",
                        api.get("/api/appointments/" + first.number("id")).string("status"),
                        "the original appointment is unchanged"));
    }

    @Test
    @DisplayName("TC-016: booking with a nonexistent service is rejected")
    void bookingWithUnknownServiceIsRejected() {
        ApiClient.Response response = api.post("/api/appointments",
                TestData.bookingBody(TestData.VALID_NAME, TestData.VALID_EMAIL, TestData.UNKNOWN_SERVICE_ID,
                        TestData.freeDate(), "10:00"));

        assertAll(
                () -> assertEquals(404, response.status()),
                () -> assertEquals("SERVICE_NOT_FOUND", response.errorCode()));
    }

    @Test
    @DisplayName("TC-021/BR-03: booking with an inactive service is rejected")
    void bookingWithInactiveServiceIsRejected() {
        ApiClient.Response response = api.post("/api/appointments",
                TestData.bookingBody(TestData.VALID_NAME, TestData.VALID_EMAIL, TestData.INACTIVE_SERVICE_ID,
                        TestData.freeDate(), "10:00"));

        assertAll(
                () -> assertEquals(409, response.status()),
                () -> assertEquals("SERVICE_INACTIVE", response.errorCode()));
    }

    @Test
    @DisplayName("TC-017: an invalid date format is rejected in a controlled way")
    void invalidDateFormatIsRejected() {
        String body = "{\"customerName\":\"" + TestData.VALID_NAME + "\",\"customerEmail\":\"" + TestData.VALID_EMAIL
                + "\",\"serviceId\":1,\"appointmentDate\":\"10-02-2027\",\"startTime\":\"10:00\"}";

        ApiClient.Response response = api.post("/api/appointments", body);

        assertAll(
                () -> assertEquals(400, response.status()),
                () -> assertEquals("INVALID_DATE_FORMAT", response.errorCode()));
    }

    @Test
    @DisplayName("TC-017: an invalid time format is rejected in a controlled way")
    void invalidTimeFormatIsRejected() {
        String body = "{\"customerName\":\"" + TestData.VALID_NAME + "\",\"customerEmail\":\"" + TestData.VALID_EMAIL
                + "\",\"serviceId\":1,\"appointmentDate\":\"" + TestData.freeDate() + "\",\"startTime\":\"10h00\"}";

        ApiClient.Response response = api.post("/api/appointments", body);

        assertAll(
                () -> assertEquals(400, response.status()),
                () -> assertEquals("INVALID_TIME_FORMAT", response.errorCode()));
    }

    @Test
    @DisplayName("TC-022/BR-05: booking outside business hours is rejected")
    void bookingOutsideBusinessHoursIsRejected() {
        LocalDate date = TestData.freeDate();

        ApiClient.Response tooEarly = api.post("/api/appointments", TestData.validBookingBody(date, "07:00"));
        ApiClient.Response tooLate = api.post("/api/appointments", TestData.validBookingBody(date, "17:00"));

        assertAll(
                () -> assertEquals(400, tooEarly.status()),
                () -> assertEquals("OUTSIDE_BUSINESS_HOURS", tooEarly.errorCode()),
                () -> assertEquals(400, tooLate.status()),
                () -> assertEquals("OUTSIDE_BUSINESS_HOURS", tooLate.errorCode()));
    }

    @Test
    @DisplayName("TC-018: a request without a JSON body returns a controlled error")
    void requestWithoutBodyIsRejected() {
        ApiClient.Response emptyBody = api.postRaw("/api/appointments", "");
        ApiClient.Response malformedJson = api.postRaw("/api/appointments", "{\"customerName\":");
        ApiClient.Response wrongType = api.postRaw("/api/appointments",
                "{\"customerName\":\"Ana\",\"customerEmail\":\"ana@example.com\",\"serviceId\":\"abc\","
                        + "\"appointmentDate\":\"" + TestData.freeDate() + "\",\"startTime\":\"10:00\"}");

        assertAll(
                () -> assertEquals(400, emptyBody.status()),
                () -> assertEquals("MISSING_BODY", emptyBody.errorCode()),
                () -> assertEquals(400, malformedJson.status()),
                () -> assertEquals("MALFORMED_REQUEST", malformedJson.errorCode()),
                () -> assertEquals(400, wrongType.status()),
                () -> assertEquals("MALFORMED_REQUEST", wrongType.errorCode()));
    }
}
