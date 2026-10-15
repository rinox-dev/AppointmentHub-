package com.appointmenthub.ui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointmenthub.support.TestData;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * UI-004 / TC-008: the interface must not confirm a booking that the server
 * rejected (FR-08, BR-07, BR-12).
 */
class DuplicateBookingUiTest extends BaseUiTest {

    private String firstSlotFromApi(LocalDate date) {
        return api.get("/api/availability?serviceId=" + TestData.ACTIVE_SERVICE_ID + "&date=" + date)
                .json().getAsJsonArray("slots").get(0).getAsJsonObject().get("startTime").getAsString();
    }

    @Test
    @DisplayName("UI-004/TC-008/BR-12: a slot taken by another customer is rejected without a confirmation")
    void slotTakenByAnotherCustomerIsRejected() {
        LocalDate date = freeDate();
        String startTime = firstSlotFromApi(date);

        home.open()
                .selectService("General Consultation")
                .setDate(date)
                .clickCheckAvailability()
                .selectSlot(startTime)
                .fillCustomer("Ana Example", "ana@example.com");

        // Another customer confirms the same slot before this user submits the form.
        int otherCustomerAppointment = bookViaApi(date, startTime);

        home.submitBooking().waitForErrorMessage();

        String error = home.errorMessage().orElseThrow();
        home.clickRefreshAppointments();

        assertAll(
                () -> assertTrue(error.contains("already occupied"), "the reason is explained: " + error),
                () -> assertTrue(home.successMessage().isEmpty(),
                        "BR-12: the interface does not display a confirmation"),
                () -> assertEquals(1, home.appointmentRows().size(),
                        "the table shows only the appointment that really exists"),
                () -> assertEquals(1, api.get("/api/appointments").jsonArray().size(),
                        "no duplicate appointment is created"),
                () -> assertEquals("CONFIRMED", api.get("/api/appointments/" + otherCustomerAppointment).string("status"),
                        "the first appointment is unchanged"));
    }

    @Test
    @DisplayName("UI-004: a slot that is no longer free disappears from the interface")
    void unavailableSlotIsNotOffered() {
        LocalDate date = freeDate();
        String startTime = firstSlotFromApi(date);
        home.open().selectService("General Consultation").setDate(date).clickCheckAvailability();

        assertTrue(home.availableSlots().contains(startTime), "the slot is offered before it is booked");

        bookViaApi(date, startTime);
        home.clickCheckAvailability();

        assertFalse(home.availableSlots().contains(startTime), "the slot is no longer offered");
    }
}
