package com.appointmenthub.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointmenthub.support.ApiClient;
import com.appointmenthub.support.ApiTestBase;
import com.appointmenthub.support.TestData;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * API tests for {@code DELETE /api/appointments/{id}} (FR-07, FR-11, FR-13, FR-14,
 * TC-011, TC-012, TC-013).
 */
@Tag("api")
class CancellationApiTest extends ApiTestBase {

    @Test
    @DisplayName("TC-011/AC-08/AC-09: cancelling a confirmed appointment changes its status and frees the slot")
    void cancellingConfirmedAppointmentReleasesTheSlot() {
        LocalDate date = TestData.freeDate();
        String startTime = firstAvailableSlot(TestData.ACTIVE_SERVICE_ID, date);
        int appointmentId = api.post("/api/appointments", TestData.validBookingBody(date, startTime)).number("id");

        ApiClient.Response cancellation = api.delete("/api/appointments/" + appointmentId);

        assertAll(
                () -> assertEquals(200, cancellation.status()),
                () -> assertEquals(appointmentId, cancellation.number("id")),
                () -> assertEquals("CANCELLED", cancellation.string("status")),
                () -> assertEquals(date.toString(), cancellation.string("appointmentDate")),
                () -> assertEquals("CANCELLED",
                        api.get("/api/appointments/" + appointmentId).string("status"),
                        "the change is persisted"),
                () -> assertTrue(availableSlots(date).contains(startTime), "the time slot is available again"));
    }

    @Test
    @DisplayName("TC-014/BR-10: a released slot can be booked again and receives a new identifier")
    void releasedSlotCanBeBookedAgain() {
        LocalDate date = TestData.freeDate();
        String startTime = firstAvailableSlot(TestData.ACTIVE_SERVICE_ID, date);
        int firstId = api.post("/api/appointments", TestData.validBookingBody(date, startTime)).number("id");
        api.delete("/api/appointments/" + firstId);

        ApiClient.Response rebooking = api.post("/api/appointments",
                TestData.bookingBody(TestData.SECOND_NAME, TestData.SECOND_EMAIL, TestData.ACTIVE_SERVICE_ID, date, startTime));

        assertAll(
                () -> assertEquals(201, rebooking.status()),
                () -> assertEquals("CONFIRMED", rebooking.string("status")),
                () -> assertNotEquals(firstId, rebooking.number("id")),
                () -> assertEquals("CANCELLED", api.get("/api/appointments/" + firstId).string("status"),
                        "the cancelled appointment keeps its own status"));
    }

    @Test
    @DisplayName("TC-012/FR-14: cancelling an already cancelled appointment is rejected")
    void cancellingTwiceIsRejected() {
        LocalDate date = TestData.freeDate();
        int appointmentId = api.post("/api/appointments",
                TestData.validBookingBody(date, firstAvailableSlot(TestData.ACTIVE_SERVICE_ID, date))).number("id");
        api.delete("/api/appointments/" + appointmentId);

        ApiClient.Response secondCancellation = api.delete("/api/appointments/" + appointmentId);

        assertAll(
                () -> assertEquals(409, secondCancellation.status()),
                () -> assertEquals("APPOINTMENT_ALREADY_CANCELLED", secondCancellation.errorCode()),
                () -> assertEquals("CANCELLED", api.get("/api/appointments/" + appointmentId).string("status")));
    }

    @Test
    @DisplayName("TC-013/FR-13: cancelling a nonexistent appointment returns 404")
    void cancellingNonexistentAppointmentReturnsNotFound() {
        ApiClient.Response response = api.delete("/api/appointments/" + TestData.UNKNOWN_APPOINTMENT_ID);

        assertAll(
                () -> assertEquals(404, response.status()),
                () -> assertEquals("APPOINTMENT_NOT_FOUND", response.errorCode()));
    }

    private String availableSlots(LocalDate date) {
        StringBuilder builder = new StringBuilder();
        api.get("/api/availability?serviceId=" + TestData.ACTIVE_SERVICE_ID + "&date=" + date)
                .json().getAsJsonArray("slots")
                .forEach(slot -> builder.append(slot.getAsJsonObject().get("startTime").getAsString()).append(','));
        return builder.toString();
    }
}
