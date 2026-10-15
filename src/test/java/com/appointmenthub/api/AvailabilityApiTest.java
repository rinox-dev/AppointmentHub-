package com.appointmenthub.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointmenthub.support.ApiClient;
import com.appointmenthub.support.ApiTestBase;
import com.appointmenthub.support.TestData;
import com.google.gson.JsonArray;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * API tests for {@code GET /api/availability} (FR-02, TC-002, TC-014).
 */
@Tag("api")
class AvailabilityApiTest extends ApiTestBase {

    @Test
    @DisplayName("TC-002: availability for a valid service and date returns slots")
    void availabilityForValidDateReturnsSlots() {
        LocalDate date = TestData.freeDate();
        ApiClient.Response response = api.get("/api/availability?serviceId=1&date=" + date);

        assertAll(
                () -> assertEquals(200, response.status()),
                () -> assertEquals(1, response.number("serviceId")),
                () -> assertEquals(date.toString(), response.string("date")),
                () -> assertEquals(30, response.number("durationMinutes")),
                () -> assertTrue(response.json().getAsJsonArray("slots").size() > 0),
                () -> assertTrue(response.string("serviceName").contains("Consultation")));
    }

    @Test
    @DisplayName("TC-002: every returned slot matches the service duration and business hours")
    void slotsRespectDurationAndBusinessHours() {
        JsonArray slots = api.get("/api/availability?serviceId=3&date=" + TestData.freeDate())
                .json().getAsJsonArray("slots");

        assertTrue(slots.size() > 0, "a 60 minute service still has slots");
        for (int index = 0; index < slots.size(); index++) {
            var slot = slots.get(index).getAsJsonObject();
            String start = slot.get("startTime").getAsString();
            String end = slot.get("endTime").getAsString();
            assertAll(
                    () -> assertTrue(start.compareTo("09:00") >= 0, "no slot starts before business hours: " + start),
                    () -> assertTrue(end.compareTo("17:00") <= 0, "no slot ends after business hours: " + end),
                    () -> assertEquals(60, minutesBetween(start, end), "slot length matches the service"));
        }
    }

    @Test
    @DisplayName("FR-02: availability for a past date is empty")
    void availabilityForPastDateIsEmpty() {
        ApiClient.Response response = api.get("/api/availability?serviceId=1&date=" + TestData.pastDate());

        assertAll(
                () -> assertEquals(200, response.status()),
                () -> assertEquals(0, response.json().getAsJsonArray("slots").size()));
    }

    @Test
    @DisplayName("FR-02: missing or invalid query parameters are rejected")
    void invalidQueryParametersAreRejected() {
        ApiClient.Response missingService = api.get("/api/availability?date=2027-02-11");
        ApiClient.Response missingDate = api.get("/api/availability?serviceId=1");
        ApiClient.Response nonNumericService = api.get("/api/availability?serviceId=abc&date=2027-02-11");
        ApiClient.Response badDate = api.get("/api/availability?serviceId=1&date=11-02-2027");

        assertAll(
                () -> assertEquals(400, missingService.status()),
                () -> assertEquals("VALIDATION_ERROR", missingService.errorCode()),
                () -> assertEquals(400, missingDate.status()),
                () -> assertEquals("VALIDATION_ERROR", missingDate.errorCode()),
                () -> assertEquals(400, nonNumericService.status()),
                () -> assertEquals(400, badDate.status()),
                () -> assertEquals("INVALID_DATE_FORMAT", badDate.errorCode()));
    }

    @Test
    @DisplayName("TC-016: availability for a nonexistent or inactive service is rejected")
    void unknownAndInactiveServicesAreRejected() {
        ApiClient.Response unknown = api.get("/api/availability?serviceId=99&date=2027-02-11");
        ApiClient.Response inactive = api.get("/api/availability?serviceId=5&date=2027-02-11");

        assertAll(
                () -> assertEquals(404, unknown.status()),
                () -> assertEquals("SERVICE_NOT_FOUND", unknown.errorCode()),
                () -> assertEquals(409, inactive.status()),
                () -> assertEquals("SERVICE_INACTIVE", inactive.errorCode()));
    }

    @Test
    @DisplayName("BR-07: a booked slot is no longer offered as available")
    void bookedSlotIsRemovedFromAvailability() {
        LocalDate date = TestData.freeDate();
        int before = availableSlotCount(1, date);
        String startTime = firstAvailableSlot(1, date);

        ApiClient.Response booking = api.post("/api/appointments", TestData.validBookingBody(date, startTime));
        int after = availableSlotCount(1, date);

        assertAll(
                () -> assertEquals(201, booking.status()),
                () -> assertEquals(before - 1, after, "the booked slot is no longer available"),
                () -> assertFalse(offeredSlots(date).contains(startTime)));
    }

    @Test
    @DisplayName("TC-014: a released slot is offered again after cancellation")
    void releasedSlotIsOfferedAgain() {
        LocalDate date = TestData.freeDate();
        String startTime = firstAvailableSlot(1, date);
        int appointmentId = api.post("/api/appointments", TestData.validBookingBody(date, startTime)).number("id");

        api.delete("/api/appointments/" + appointmentId);

        assertAll(
                () -> assertTrue(offeredSlots(date).contains(startTime),
                        "the cancelled slot is available again"),
                () -> assertEquals(201, api.post("/api/appointments",
                        TestData.bookingBody(TestData.SECOND_NAME, TestData.SECOND_EMAIL, 1, date, startTime)).status()));
    }

    private String offeredSlots(LocalDate date) {
        StringBuilder builder = new StringBuilder();
        api.get("/api/availability?serviceId=1&date=" + date).json().getAsJsonArray("slots")
                .forEach(slot -> builder.append(slot.getAsJsonObject().get("startTime").getAsString()).append(','));
        return builder.toString();
    }

    private int minutesBetween(String start, String end) {
        int startMinutes = Integer.parseInt(start.substring(0, 2)) * 60 + Integer.parseInt(start.substring(3));
        int endMinutes = Integer.parseInt(end.substring(0, 2)) * 60 + Integer.parseInt(end.substring(3));
        return endMinutes - startMinutes;
    }
}
