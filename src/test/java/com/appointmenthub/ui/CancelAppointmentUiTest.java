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
 * UI-005 / TC-020: cancelling an appointment through the interface
 * (FR-07, FR-11, FR-12, AC-09).
 */
class CancelAppointmentUiTest extends BaseUiTest {

    private String firstSlotFromApi(LocalDate date) {
        return api.get("/api/availability?serviceId=" + TestData.ACTIVE_SERVICE_ID + "&date=" + date)
                .json().getAsJsonArray("slots").get(0).getAsJsonObject().get("startTime").getAsString();
    }

    @Test
    @DisplayName("UI-005/TC-020: cancelling through the interface updates the status and frees the slot")
    void cancellingThroughTheInterfaceReleasesTheSlot() {
        LocalDate date = freeDate();
        String startTime = firstSlotFromApi(date);
        int appointmentId = bookViaApi(date, startTime);

        home.open().selectService("General Consultation").setDate(date).clickCheckAvailability();
        home.waitForAppointment(appointmentId);

        assertAll(
                () -> assertEquals("CONFIRMED", home.appointmentStatus(appointmentId)),
                () -> assertTrue(home.availableSlots().size() > 0, "other slots are offered"),
                () -> assertFalse(home.availableSlots().contains(startTime), "the slot is occupied"));

        home.cancelAppointment(appointmentId).waitForStatus(appointmentId, "CANCELLED");

        assertAll(
                () -> assertEquals("CANCELLED", home.appointmentStatus(appointmentId)),
                () -> assertTrue(home.successMessage().orElseThrow().contains("cancelled"),
                        "the user is informed about the result"),
                () -> assertEquals("CANCELLED", api.get("/api/appointments/" + appointmentId).string("status"),
                        "the server state matches the interface"),
                () -> assertTrue(home.clickCheckAvailability().availableSlots().contains(startTime),
                        "the released slot is offered again"));
    }

    @Test
    @DisplayName("UI-005: the cancel action is disabled for an already cancelled appointment")
    void cancelIsDisabledAfterCancellation() {
        LocalDate date = freeDate();
        int appointmentId = bookViaApi(date, firstSlotFromApi(date));

        home.open().waitForAppointment(appointmentId);
        home.cancelAppointment(appointmentId).waitForStatus(appointmentId, "CANCELLED");

        boolean cancelDisabled = home.appointmentRow(appointmentId).orElseThrow()
                .findElement(org.openqa.selenium.By.cssSelector("[data-testid='cancel-button']"))
                .getAttribute("disabled") != null;

        assertAll(
                () -> assertTrue(cancelDisabled, "the button is disabled for a cancelled appointment"),
                () -> assertEquals("CANCELLED", api.get("/api/appointments/" + appointmentId).string("status"),
                        "the appointment stays cancelled on the server"),
                () -> assertEquals("CANCELLED", home.appointmentStatus(appointmentId)));
    }
}
